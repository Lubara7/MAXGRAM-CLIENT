#version 460 core
#include <flutter/runtime_effect.glsl>
uniform vec2 uSize;
uniform vec4 uColor0;
uniform vec4 uColor1;
uniform vec4 uColor2;
uniform vec4 uColor3;
out vec4 fragColor;
float weightAt(vec2 point, vec2 center) {
  float weight = max(0.0, 0.9 - distance(point, center));
  return weight * weight * weight * weight;
}
void main() {
  vec2 delta = FlutterFragCoord().xy / uSize - vec2(0.5);
  float swirl = 0.35 * length(delta);
  float angle = swirl * swirl * 0.8 * 8.0;
  float sine = sin(angle);
  float cosine = cos(angle);
  vec2 point = clamp(vec2(0.5) + vec2(delta.x * cosine - delta.y * sine, delta.x * sine + delta.y * cosine), vec2(0.0), vec2(1.0));
  float w0 = weightAt(point, vec2(0.80, 0.90));
  float w1 = weightAt(point, vec2(0.35, 0.75));
  float w2 = weightAt(point, vec2(0.20, 0.10));
  float w3 = weightAt(point, vec2(0.65, 0.25));
  vec3 mixed = uColor0.rgb * w0 + uColor1.rgb * w1 + uColor2.rgb * w2 + uColor3.rgb * w3;
  fragColor = vec4(mixed / max(w0 + w1 + w2 + w3, 0.0001), 1.0);
}
