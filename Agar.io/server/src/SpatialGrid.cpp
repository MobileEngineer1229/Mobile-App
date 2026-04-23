#include "SpatialGrid.h"
#include <cmath>
#include <algorithm>

SpatialGrid::SpatialGrid(float world_w, float world_h)
    : world_w_(world_w), world_h_(world_h) {}

void SpatialGrid::clear() {
    cells_.clear();
    entity_cell_.clear();
}

void SpatialGrid::insert(EntityID id, float x, float y) {
    auto [cx, cy] = cell_of(x, y);
    CellKey k = key(cx, cy);
    cells_[k].push_back(id);
    entity_cell_[id] = k;
}

void SpatialGrid::move(EntityID id, float ox, float oy,
                                    float nx, float ny) {
    auto [ocx, ocy] = cell_of(ox, oy);
    auto [ncx, ncy] = cell_of(nx, ny);

    if (ocx == ncx && ocy == ncy) return; // same cell — nothing to do

    // Remove from old cell
    CellKey old_k = key(ocx, ocy);
    auto it = cells_.find(old_k);
    if (it != cells_.end()) {
        auto& vec = it->second;
        vec.erase(std::remove(vec.begin(), vec.end(), id), vec.end());
    }

    // Insert into new cell
    CellKey new_k = key(ncx, ncy);
    cells_[new_k].push_back(id);
    entity_cell_[id] = new_k;
}

void SpatialGrid::remove(EntityID id, float x, float y) {
    auto [cx, cy] = cell_of(x, y);
    CellKey k = key(cx, cy);
    auto it = cells_.find(k);
    if (it != cells_.end()) {
        auto& vec = it->second;
        vec.erase(std::remove(vec.begin(), vec.end(), id), vec.end());
    }
    entity_cell_.erase(id);
}

std::vector<EntityID> SpatialGrid::query_aoi(float x, float y) const {
    std::vector<EntityID> result;
    result.reserve(128);
    auto [cx, cy] = cell_of(x, y);

    // Check current cell + 8 neighbors (per IO-Server.md)
    for (int dx = -1; dx <= 1; ++dx)
    for (int dy = -1; dy <= 1; ++dy) {
        CellKey k = key(cx + dx, cy + dy);
        auto it = cells_.find(k);
        if (it != cells_.end()) {
            result.insert(result.end(),
                          it->second.begin(), it->second.end());
        }
    }
    return result;
}

std::vector<EntityID> SpatialGrid::query_radius(
        float cx, float cy, float radius) const {
    auto candidates = query_aoi(cx, cy);
    std::vector<EntityID> result;
    result.reserve(candidates.size());
    float r2 = radius * radius;
    // Caller must map EntityID → Pos to do exact distance check.
    // We return broad-phase candidates here.
    return candidates; // narrow phase done in Systems.cpp
}
