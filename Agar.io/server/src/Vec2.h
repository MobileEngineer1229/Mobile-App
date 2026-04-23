#pragma once
#include <cmath>

struct Vec2 {
    float x = 0.f, y = 0.f;

    Vec2() = default;
    Vec2(float x, float y) : x(x), y(y) {}

    Vec2  operator+(const Vec2& o) const { return {x + o.x, y + o.y}; }
    Vec2  operator-(const Vec2& o) const { return {x - o.x, y - o.y}; }
    Vec2  operator*(float s)       const { return {x * s,   y * s};   }
    Vec2& operator+=(const Vec2& o)      { x += o.x; y += o.y; return *this; }
    Vec2& operator-=(const Vec2& o)      { x -= o.x; y -= o.y; return *this; }

    float lengthSq()                const { return x*x + y*y; }
    float length()                  const { return std::sqrt(lengthSq()); }

    Vec2 normalized() const {
        float l = length();
        if (l < 1e-6f) return {0.f, 0.f};
        return {x / l, y / l};
    }

    float dot(const Vec2& o) const { return x*o.x + y*o.y; }

    static float dist(const Vec2& a, const Vec2& b) {
        return (a - b).length();
    }
};
