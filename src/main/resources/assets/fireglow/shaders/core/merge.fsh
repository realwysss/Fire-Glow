#version 330

uniform sampler2D InSampler;
uniform sampler2D MaskSampler;

layout(std140) uniform Merge {
    vec4 Fire;
    vec4 Soul;
    vec4 Look;
    vec4 Size;
    vec4 FireTint;
    vec4 SoulTint;
};

in vec2 texCoord;

out vec4 fragColor;

vec3 hsv(vec3 c) {
    vec4 k = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
    vec4 p = mix(vec4(c.bg, k.wz), vec4(c.gb, k.xy), step(c.b, c.g));
    vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));
    float d = q.x - min(q.w, q.y);
    return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + 1.0e-10)), d / (q.x + 1.0e-10), q.x);
}

vec3 rgb(vec3 c) {
    vec3 p = abs(fract(c.xxx + vec3(1.0, 2.0 / 3.0, 1.0 / 3.0)) * 6.0 - 3.0);
    return c.z * mix(vec3(1.0), clamp(p - 1.0, 0.0, 1.0), c.y);
}

vec3 paint(vec3 tint, float l) {
    vec3 t = hsv(tint);
    float h = t.x + (l - 0.5) * 0.14;
    float s = t.y * (1.0 - 0.85 * smoothstep(0.78, 1.0, l));
    float v = t.z * clamp(0.45 + 0.75 * l, 0.0, 1.0);
    return rgb(vec3(h, s, v));
}

void main() {
    vec3 b = clamp(texture(InSampler, texCoord).rgb, 0.0, 1.0);
    vec4 m = texture(MaskSampler, texCoord);
    float over = mix(1.0 - m.b, 1.0, Look.x);
    vec3 g = (Fire.rgb * pow(b.r, Fire.a) + Soul.rgb * pow(b.g, Soul.a)) * over;
    if (Look.z > 0.0) {
        vec2 p = texCoord * Size.xy / Size.y;
        float band = pow(0.5 + 0.5 * sin((p.x * 0.8 + p.y) * Size.z - Look.y * 3.2), 10.0);
        g *= 1.0 + Look.z * band * 2.2;
    }
    g = 1.0 - exp(-g);
    float w = m.r + m.g;
    vec4 tint = w > 0.0001 ? (FireTint * m.r + SoulTint * m.g) / w : vec4(0.0);
    float e = clamp(w * 1.6, 0.0, 1.0) * tint.a;
    float l = clamp((m.a - 0.02) / 0.98, 0.0, 1.0);
    vec3 c = min(mix(g, paint(tint.rgb, l) + g * 0.35, e), vec3(1.0));
    float a = max(e, Look.w * max(g.r, max(g.g, g.b)));
    fragColor = vec4(c, a);
}
