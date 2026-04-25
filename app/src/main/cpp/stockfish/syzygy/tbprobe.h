#ifndef TBPROBE_H_INCLUDED
#define TBPROBE_H_INCLUDED

#include <functional>
#include <string>
#include <vector>
#include <ostream>
#include "../types.h"
#include "../position.h"

namespace Stockfish {
    class OptionsMap;
    namespace Search { struct RootMove; struct LimitsType; }
}

namespace Stockfish::Tablebases {
    enum class ProbeState { FAIL, OK };

    inline std::ostream& operator<<(std::ostream& os, ProbeState) {
        os << "FAIL";
        return os;
    }

    struct Config {
        int cardinality = 0;
        int probeDepth = 0;
        bool useRule50 = false;
        bool rootInTB = false;
    };

    inline int MaxCardinality = 0;

    using WDLScore = int;
    inline void init(const std::string&) {}
    inline WDLScore probe_wdl(const Position&, ProbeState* err) { if (err) *err = ProbeState::FAIL; return 0; }
    inline int probe_dtz(const Position&, ProbeState* err) { if (err) *err = ProbeState::FAIL; return 0; }

    inline Config rank_root_moves(const OptionsMap&, Position&, std::vector<Search::RootMove>&, bool = false, std::function<bool()> = []{return false;}) {
        return {};
    }
}

#endif
