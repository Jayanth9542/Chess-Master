#ifndef NNUE_ACCUMULATOR_H_INCLUDED
#define NNUE_ACCUMULATOR_H_INCLUDED

#include <tuple>
#include <vector>
#include "../types.h"

namespace Stockfish {
class Position;

namespace Eval::NNUE {

struct Networks;

struct AccumulatorStack {
    void reset() {}
    std::pair<DirtyPiece, DirtyThreats> push() { return {}; }
    void pop() {}
};

struct AccumulatorCaches {
    AccumulatorCaches(const Networks&) {}
    void clear(const Networks&) {}
    struct Cache {} small, big;
};

} // namespace Eval::NNUE
} // namespace Stockfish

#endif
