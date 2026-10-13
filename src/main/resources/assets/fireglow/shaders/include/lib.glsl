#version 330

float hue(vec3 c) {
    float hi = max(c.r, max(c.g, c.b));
    float lo = min(c.r, min(c.g, c.b));
    float d = hi - lo;
    if (d < 0.00001) {
        return 0.0;
    }
    float h;
    if (hi == c.r) {
        h = mod((c.g - c.b) / d, 6.0);
    } else if (hi == c.g) {
        h = (c.b - c.r) / d + 2.0;
    } else {
        h = (c.r - c.g) / d + 4.0;
    }
    return h / 6.0;
}

float sat(vec3 c) {
    float hi = max(c.r, max(c.g, c.b));
    return hi > 0.0 ? (hi - min(c.r, min(c.g, c.b))) / hi : 0.0;
}

float band(vec3 c) {
    float h = hue(c);
    return smoothstep(0.36, 0.42, h) * (1.0 - smoothstep(0.72, 0.78, h));
}

float heat(vec3 c) {
    float hi = max(c.r, max(c.g, c.b));
    return clamp(smoothstep(0.42, 0.55, hi) * smoothstep(0.7, 0.85, sat(c)) + smoothstep(0.8, 0.95, hi), 0.0, 1.0);
}

vec4 flame(sampler2D tex, vec2 uv, vec3 c) {
    float s = sat(c);
    float soul = band(c) * smoothstep(0.03, 0.1, s);
    if (s < 0.3) {
        vec2 px = 1.0 / vec2(textureSize(tex, 0));
        float ws = 0.0;
        float wb = 0.0;
        for (int y = -2; y <= 2; y++) {
            for (int x = -2; x <= 2; x++) {
                vec4 n = textureLod(tex, uv + vec2(x, y) * px, 0.0);
                float k = step(0.1, n.a) * heat(n.rgb) * sat(n.rgb);
                ws += k;
                wb += k * band(n.rgb);
            }
        }
        if (ws > 0.01) {
            soul = wb / ws;
        }
    }
    float f = heat(c);
    float l = dot(c, vec3(0.2126, 0.7152, 0.0722));
    return vec4(f * (1.0 - soul), f * soul, 1.0, 0.02 + 0.98 * l);
}
