#ifndef NETWORK_H_INCLUDED
#define NETWORK_H_INCLUDED

#include <string>
#include <tuple>
#include <vector>
#include <functional>
#include <string_view>
#include <optional>

namespace Stockfish {
class Position;

namespace Eval::NNUE {

struct AccumulatorStack;
struct AccumulatorCaches;

struct EvalFile {
    std::string name;
    std::string type;
    std::string path;
};

struct Networks {
    struct Net {
        Net() = default;
        Net(const EvalFile&) {}
        template<typename... Args>
        auto evaluate(const Position&, Args...) const { return std::make_tuple(0, 0); }
        void load(const std::string&, const std::string&) {}
        void save(const std::string&) const {}
        void save(const std::optional<std::string>&) const {}
        void verify(const std::string&, const std::function<void(std::string_view)>&) const {}
    } small, big;

    Networks(const EvalFile& b, const EvalFile& s) : small(s), big(b) {}

    auto get_status_and_errors() const {
        return std::vector<std::pair<std::string, std::string>>{};
    }

    bool operator==(const Networks&) const { return true; }
};

inline std::string trace(const Position&, const Networks&, const AccumulatorCaches&) {
    return "NNUE disabled";
}

} // namespace Eval::NNUE
} // namespace Stockfish

namespace std {
template<>
struct hash<Stockfish::Eval::NNUE::Networks> {
    size_t operator()(const Stockfish::Eval::NNUE::Networks&) const { return 0; }
};
}

#endif
