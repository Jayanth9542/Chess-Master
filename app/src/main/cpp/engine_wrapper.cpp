#include "engine_wrapper.h"
#include <string>
#include <sstream>
#include <cstring>
#include <cerrno>
#include <fcntl.h>
#include <sys/time.h>
#include <sys/select.h>

extern "C" int stockfish_main(int argc, char* argv[]);

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
        int savedStdin  = dup(STDIN_FILENO);
        int savedStdout = dup(STDOUT_FILENO);

        if (dup2(self->toEngine_[0],   STDIN_FILENO)  < 0 ||
            dup2(self->fromEngine_[1], STDOUT_FILENO) < 0) {
            LOGE("dup2 failed: %s", strerror(errno));
            self->running_ = false;
            return nullptr;
        }

        // REMOVED: Do not close the pipe ends here!
        // Threads share the file descriptor table. Closing them here
        // prevents the parent thread from sending or receiving commands.

        LOGI("Stockfish thread starting");
        char progName[] = "stockfish";
        char* argv[]    = { progName, nullptr };
        stockfish_main(1, argv);

        dup2(savedStdin,  STDIN_FILENO);
        dup2(savedStdout, STDOUT_FILENO);
        close(savedStdin);
        close(savedStdout);

        self->running_ = false;
        LOGI("Stockfish thread exited cleanly");
        return nullptr;
    }

    bool EngineWrapper::initialize() {
        if (initialized_) return true;
        if (pipe(toEngine_) != 0 || pipe(fromEngine_) != 0) {
            LOGE("pipe() failed: %s", strerror(errno));
            return false;
        }
        running_ = true;
        pthread_attr_t attr;
        pthread_attr_init(&attr);
        pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_JOINABLE);
        if (pthread_create(&engineThread_, &attr, engineThreadFunc, this) != 0) {
            LOGE("pthread_create failed: %s", strerror(errno));
            running_ = false;
            pthread_attr_destroy(&attr);
            return false;
        }
        pthread_attr_destroy(&attr);
        usleep(100000);

        sendCommand("uci");
        if (!waitForToken("uciok", 6000)) {
            LOGE("No uciok received");
            return false;
        }
        sendCommand("isready");
        if (!waitForToken("readyok", 6000)) {
            LOGE("No readyok received");
            return false;
        }
        sendCommand("setoption name Hash value 64");
        sendCommand("setoption name Threads value 1");
        initialized_ = true;
        LOGI("Engine initialised successfully");
        return true;
    }

    bool EngineWrapper::sendCommand(const std::string& cmd) {
        if (!running_) return false;
        pthread_mutex_lock(&writeMutex_);
        std::string line = cmd + "\n";
        ssize_t n = write(toEngine_[1], line.c_str(), line.size());
        pthread_mutex_unlock(&writeMutex_);
        if (n < 0) { LOGE("write() failed: %s", strerror(errno)); return false; }
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
            long remainMs = (deadline.tv_sec  - now.tv_sec)  * 1000
                            + (deadline.tv_usec - now.tv_usec) / 1000;
            if (remainMs <= 0) break;
            struct timeval tv;
            tv.tv_sec  = remainMs / 1000;
            tv.tv_usec = (remainMs % 1000) * 1000;
            fd_set fds;
            FD_ZERO(&fds);
            FD_SET(fromEngine_[0], &fds);
            int ret = select(fromEngine_[0] + 1, &fds, nullptr, nullptr, &tv);
            if (ret <= 0) break;
            ssize_t nr = read(fromEngine_[0], &c, 1);
            if (nr <= 0) break;
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
            long elapsed = (now.tv_sec  - start.tv_sec)  * 1000
                           + (now.tv_usec - start.tv_usec) / 1000;
            if (elapsed >= timeoutMs) return false;
            std::string line = readLine(timeoutMs - (int)elapsed);
            if (line.find(token) != std::string::npos) return true;
        }
    }

    std::string EngineWrapper::getBestMove(const std::string& fenPosition,
                                           int depth, int skillLevel,
                                           int timeLimitMs) {
        if (!initialized_ || !running_) return "";
        skillLevel = std::max(0, std::min(20, skillLevel));
        depth      = std::max(1, std::min(30, depth));
        sendCommand("setoption name Skill Level value " + std::to_string(skillLevel));
        sendCommand("position fen " + fenPosition);
        sendCommand("go depth " + std::to_string(depth));

        struct timeval start, now;
        gettimeofday(&start, nullptr);
        while (true) {
            gettimeofday(&now, nullptr);
            long elapsed = (now.tv_sec  - start.tv_sec)  * 1000
                           + (now.tv_usec - start.tv_usec) / 1000;
            if (elapsed >= timeLimitMs) { stopSearch(); break; }
            std::string line = readLine(timeLimitMs - (int)elapsed);
            if (line.find("bestmove") == std::string::npos) continue;
            std::istringstream iss(line);
            std::string tok;
            while (iss >> tok) {
                if (tok == "bestmove") {
                    std::string move;
                    if (iss >> move) {
                        if (move == "(none)") return "";
                        return move;
                    }
                }
            }
            break;
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
        struct timespec ts { 2, 0 };
        pthread_timedjoin_np(engineThread_, nullptr, &ts);
        running_     = false;
        initialized_ = false;

        // The parent gracefully cleans up all file descriptors here
        for (int fd : { toEngine_[0], toEngine_[1],
                        fromEngine_[0], fromEngine_[1] }) {
            if (fd >= 0) close(fd);
        }
        toEngine_[0] = toEngine_[1] = fromEngine_[0] = fromEngine_[1] = -1;
        LOGI("Engine shut down");
    }

    void EngineWrapper::pthread_timedjoin_np(pthread_t thread, void *pVoid, timespec *pTimespec) {

    }

} // namespace chess