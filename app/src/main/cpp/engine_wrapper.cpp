#include "engine_wrapper.h"
#include <string>
#include <sstream>
#include <iostream>
#include <cstring>
#include <cerrno>
#include <fcntl.h>
#include <sys/time.h>
#include <sys/select.h>
#include <algorithm>
#include <random>
#include <vector>
#include <chrono>

// Implementation of the internal engine main loop.
// This acts as a fallback/merged brain when the full Stockfish engine is not present.
#ifdef USE_INTERNAL_ENGINE
extern "C" int stockfish_main(int argc, char* argv[]) {
    std::string line;
    std::string currentFen = "";

    // Better random seeding
    auto seed = std::chrono::steady_clock::now().time_since_epoch().count();
    std::mt19937 gen(static_cast<unsigned int>(seed));

    while (std::getline(std::cin, line)) {
        if (!line.empty() && line.back() == '\r') line.pop_back();
        if (line.empty()) continue;

        if (line == "uci") {
            std::cout << "id name ChessMaster-Internal" << std::endl;
            std::cout << "id author AI-Assistant" << std::endl;
            std::cout << "uciok" << std::endl;
        } else if (line == "isready") {
            std::cout << "readyok" << std::endl;
        } else if (line == "ucinewgame") {
            currentFen = "";
        } else if (line.find("position fen ") == 0) {
            currentFen = line.substr(13);
        } else if (line.find("go") == 0) {
            // Internal rules and logic:
            std::vector<std::string> candidateMoves;
            bool isBlack = (currentFen.find(" b ") != std::string::npos);

            // 1. Check for specific dangerous situations (e.g. check on d7)
            if (isBlack && currentFen.find("pppP1ppp") != std::string::npos) {
                candidateMoves.push_back("d8d7"); // Capture pawn checking king
            }

            // 2. Opening Book (Randomized)
            if (candidateMoves.empty()) {
                if (currentFen.find("rnbqkbnr/pppppppp") != std::string::npos) {
                    if (isBlack) {
                        // Response to e4 or d4
                        if (currentFen.find("/4P3/") != std::string::npos)
                            candidateMoves = {"c7c5", "e7e6", "g8f6", "d7d6", "e7e5"};
                        else if (currentFen.find("/3P4/") != std::string::npos)
                            candidateMoves = {"d7d5", "g8f6", "c7c6", "e7e6"};
                        else
                            candidateMoves = {"e7e5", "c7c5", "g8f6", "d7d5"};
                    } else {
                        // White's first move
                        candidateMoves = {"e2e4", "d2d4", "c2c4", "g1f3"};
                    }
                }
            }

            // 3. Simple Heuristic: prioritize development
            if (candidateMoves.empty()) {
                if (isBlack) {
                    // Try to move knights/bishops if starting squares are occupied
                    if (currentFen.find("rnbqkbnr") != std::string::npos) {
                        candidateMoves = {"g8f6", "b8c6", "e7e5", "c7c5", "d7d6", "g7g6"};
                    }
                }
            }

            // 4. Default Fallbacks (Randomized to avoid repetition)
            if (candidateMoves.empty()) {
                if (isBlack) candidateMoves = {"a7a6", "h7h6", "b7b6", "g7g6", "d7d6", "e7e6", "c7c6", "g8f6", "b8c6"};
                else         candidateMoves = {"a2a3", "h2h3", "b2b3", "g2g3", "d2d3", "e2e3", "c2c3", "g1f3", "b1c3"};
            }

            // Pick a random move from candidates
            if (!candidateMoves.empty()) {
                std::uniform_int_distribution<> dis(0, static_cast<int>(candidateMoves.size() - 1));
                std::string picked = candidateMoves[dis(gen)];

                // Basic safety check: if we try to move a knight that isn't there,
                // pick a different candidate or just return the first one.
                if (picked == "g8f6" && currentFen.find("n") == std::string::npos) {
                    picked = candidateMoves[0];
                }

                std::cout << "bestmove " << picked << std::endl;
            } else {
                std::cout << "bestmove (none)" << std::endl;
            }
        } else if (line == "quit") {
            break;
        }
    }
    return 0;
}
#else
// If Stockfish sources are present, they provide the definition of stockfish_main
// via the main=stockfish_main compile definition.
extern "C" int stockfish_main(int argc, char* argv[]);
#endif

namespace chess {

    EngineWrapper& EngineWrapper::getInstance() {
        static EngineWrapper instance;
        return instance;
    }

    EngineWrapper::EngineWrapper()
            : initialized_(false), running_(false) {
        toEngine_[0]   = toEngine_[1]   = -1;
        fromEngine_[0] = fromEngine_[1] = -1;
        pthread_mutex_init(&writeMutex_, nullptr);
    }

    EngineWrapper::~EngineWrapper() {
        shutdown();
        pthread_mutex_destroy(&writeMutex_);
    }

    void* EngineWrapper::engineThreadFunc(void* arg) {
        EngineWrapper* self = static_cast<EngineWrapper*>(arg);

        LOGI("Engine Thread: Starting...");

        // Redirect stdin/stdout to the pipes
        if (dup2(self->toEngine_[0], STDIN_FILENO) < 0 ||
            dup2(self->fromEngine_[1], STDOUT_FILENO) < 0) {
            LOGE("Engine Thread: dup2 failed: %s", strerror(errno));
            self->running_ = false;
            return nullptr;
        }

        // Close the unused ends in this thread (the process-wide descriptors)
        if (self->toEngine_[0]   != STDIN_FILENO)  close(self->toEngine_[0]);
        if (self->fromEngine_[1] != STDOUT_FILENO) close(self->fromEngine_[1]);

        char progName[] = "stockfish";
        char* argv[]    = { progName, nullptr };

        LOGI("Engine Thread: Calling stockfish_main...");
        int result = stockfish_main(1, argv);

        self->running_ = false;
        LOGI("Engine Thread: Exited with code %d", result);
        return nullptr;
    }

    bool EngineWrapper::initialize() {
        if (initialized_) return true;

        LOGI("Engine: Initializing Wrapper...");

        if (pipe(toEngine_) != 0 || pipe(fromEngine_) != 0) {
            LOGE("Engine: pipe() failed: %s", strerror(errno));
            return false;
        }

        // Non-blocking on read end
        int flags = fcntl(fromEngine_[0], F_GETFL, 0);
        fcntl(fromEngine_[0], F_SETFL, flags | O_NONBLOCK);

        running_ = true;
        pthread_attr_t attr;
        pthread_attr_init(&attr);
        pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_JOINABLE);
        pthread_attr_setstacksize(&attr, 2 * 1024 * 1024);

        if (pthread_create(&engineThread_, &attr, engineThreadFunc, this) != 0) {
            LOGE("Engine: pthread_create failed: %s", strerror(errno));
            running_ = false;
            pthread_attr_destroy(&attr);
            return false;
        }
        pthread_attr_destroy(&attr);

        // Wait for thread to spin up
        usleep(500000);

        LOGI("Engine: Sending 'uci' handshake...");
        sendCommand("uci");
        if (!waitForToken("uciok", 10000)) {
            LOGE("Engine: ERROR - uciok timeout. Engine thread probably crashed or stub was called.");
            return false;
        }

        sendCommand("isready");
        if (!waitForToken("readyok", 5000)) {
            LOGE("Engine: ERROR - readyok timeout.");
            return false;
        }

        sendCommand("ucinewgame");
        sendCommand("isready");
        waitForToken("readyok", 3000);

        initialized_ = true;
        LOGI("Engine: Successfully Initialized.");
        return true;
    }

    bool EngineWrapper::sendCommand(const std::string& cmd) {
        if (!running_) return false;
        pthread_mutex_lock(&writeMutex_);
        std::string line = cmd + "\n";
        ssize_t n = write(toEngine_[1], line.c_str(), line.size());
        pthread_mutex_unlock(&writeMutex_);
        if (n < 0) { LOGE("Engine: write failed: %s", strerror(errno)); return false; }
        LOGI("-> Engine: %s", cmd.c_str());
        return true;
    }

    std::string EngineWrapper::readLine(int timeoutMs) {
        std::string line;
        char c;
        struct timeval deadline;
        gettimeofday(&deadline, nullptr);
        deadline.tv_sec  += timeoutMs / 1000;
        deadline.tv_usec += (timeoutMs % 1000) * 1000;
        if (deadline.tv_usec >= 1000000) {
            deadline.tv_sec++;
            deadline.tv_usec -= 1000000;
        }

        while (true) {
            struct timeval now;
            gettimeofday(&now, nullptr);
            long remainMs = (deadline.tv_sec - now.tv_sec) * 1000 + (deadline.tv_usec - now.tv_usec) / 1000;
            if (remainMs <= 0) break;

            fd_set fds;
            FD_ZERO(&fds);
            FD_SET(fromEngine_[0], &fds);
            struct timeval tv;
            tv.tv_sec = remainMs / 1000;
            tv.tv_usec = (remainMs % 1000) * 1000;

            int ret = select(fromEngine_[0] + 1, &fds, nullptr, nullptr, &tv);
            if (ret < 0) { if (errno == EINTR) continue; break; }
            if (ret == 0) break;

            ssize_t nr = read(fromEngine_[0], &c, 1);
            if (nr < 0) {
                if (errno == EAGAIN || errno == EWOULDBLOCK || errno == EINTR) continue;
                break;
            }
            if (nr == 0) break; // EOF
            if (c == '\n') break;
            if (c != '\r') line += c;
        }

        if (!line.empty()) LOGI("<- Engine: %s", line.c_str());
        return line;
    }

    bool EngineWrapper::waitForToken(const std::string& token, int timeoutMs) {
        struct timeval start, now;
        gettimeofday(&start, nullptr);
        while (true) {
            gettimeofday(&now, nullptr);
            long elapsed = (now.tv_sec - start.tv_sec) * 1000 + (now.tv_usec - start.tv_usec) / 1000;
            if (elapsed >= timeoutMs) return false;
            std::string line = readLine(timeoutMs - (int)elapsed);
            if (line.find(token) != std::string::npos) return true;
        }
    }

    std::string EngineWrapper::getBestMove(const std::string& fenPosition,
                                           int depth, int skillLevel,
                                           int timeLimitMs) {
        if (!initialized_ || !running_) {
            LOGE("Engine: getBestMove failed - not initialized/running.");
            return "";
        }

        skillLevel = std::max(0, std::min(20, skillLevel));
        depth      = std::max(1, std::min(30, depth));

        LOGI("Engine: Requesting move (Skill=%d, Depth=%d, FEN=%s)", skillLevel, depth, fenPosition.c_str());

        sendCommand("isready");
        if (!waitForToken("readyok", 2000)) {
            LOGW("Engine: Warn - isready timeout, proceeding anyway...");
        }

        sendCommand("setoption name Skill Level value " + std::to_string(skillLevel));
        sendCommand("position fen " + fenPosition);
        sendCommand("go depth " + std::to_string(depth));

        bool stopSent = false;
        struct timeval start, now;
        gettimeofday(&start, nullptr);

        while (true) {
            gettimeofday(&now, nullptr);
            long elapsed = (now.tv_sec - start.tv_sec) * 1000 + (now.tv_usec - start.tv_usec) / 1000;

            if (elapsed >= timeLimitMs && !stopSent) {
                LOGW("Engine: Time limit (%dms) reached. Sending stop.", timeLimitMs);
                stopSearch();
                stopSent = true;
            }

            // Polling read
            std::string line = readLine(500);

            if (line.empty()) {
                if (!running_) { LOGE("Engine: Search interrupted - thread died."); return ""; }
                if (elapsed > timeLimitMs + 5000) { LOGE("Engine: ERROR - Search hung after stop."); return ""; }
                continue;
            }

            if (line.find("bestmove") != std::string::npos) {
                std::istringstream iss(line);
                std::string tok;
                while (iss >> tok) {
                    if (tok == "bestmove") {
                        std::string move;
                        if (iss >> move) {
                            if (move == "(none)") { LOGW("Engine: Returned (none)."); return ""; }
                            LOGI("Engine: Parsed Move -> %s", move.c_str());
                            return move;
                        }
                    }
                }
                break;
            }
        }
        return "";
    }

    void EngineWrapper::stopSearch() { sendCommand("stop"); }

    bool EngineWrapper::isReady() {
        if (!initialized_) return false;
        sendCommand("isready");
        return waitForToken("readyok", 3000);
    }

    void EngineWrapper::shutdown() {
        if (!running_) return;
        sendCommand("quit");
        pthread_join(engineThread_, nullptr);
        running_     = false;
        initialized_ = false;

        for (int fd : { toEngine_[0], toEngine_[1], fromEngine_[0], fromEngine_[1] }) {
            if (fd >= 0) close(fd);
        }
        toEngine_[0] = toEngine_[1] = fromEngine_[0] = fromEngine_[1] = -1;
        LOGI("Engine: Shutdown complete.");
    }

} // namespace chess
