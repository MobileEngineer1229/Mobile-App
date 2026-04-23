#pragma once
// ─────────────────────────────────────────────────────────────
//  Static Grid  (per IO-Server.md recommendation)
//  Faster than quadtree for IO games because entities move
//  every single frame.  Cell size = largest AoI radius.
//  AoI check: current cell + 8 surrounding cells only.
// ─────────────────────────────────────────────────────────────
#include <vector>
#include <cstdint>
#include <unordered_map>
#include <functional>
#include "Config.h"

using EntityID = uint32_t;

class SpatialGrid {
public:
    static constexpr float CELL_SIZE = 1000.f; // matches AoI radius

    SpatialGrid(float world_w = Config::WORLD_W,
                float world_h = Config::WORLD_H);

    void clear();

    // Insert entity at position
    void insert(EntityID id, float x, float y);

    // Update entity position (removes old, inserts new)
    void move(EntityID id, float old_x, float old_y,
                           float new_x, float new_y);

    // Remove entity
    void remove(EntityID id, float x, float y);

    // Query all entities in AoI (current + 8 neighbors)
    std::vector<EntityID> query_aoi(float x, float y) const;

    // Query exact radius (filtered from AoI)
    std::vector<EntityID> query_radius(float cx, float cy, float radius) const;

private:
    using CellKey = uint64_t;

    CellKey key(int cx, int cy) const {
        return (static_cast<uint64_t>(cx + 32768) << 32)
             |  static_cast<uint64_t>(cy + 32768);
    }

    std::pair<int,int> cell_of(float x, float y) const {
        return { static_cast<int>(x / CELL_SIZE),
                 static_cast<int>(y / CELL_SIZE) };
    }

    std::unordered_map<CellKey, std::vector<EntityID>> cells_;
    // reverse map: entity → last cell (for fast remove/move)
    std::unordered_map<EntityID, CellKey> entity_cell_;

    float world_w_, world_h_;
};
