#version 330

#moj_import <fireglow:lib.glsl>

uniform sampler2D InSampler;

layout(std140) uniform Pass {
    vec4 A;
    vec4 B;
};

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec2 p = texCoord * B.x - B.y;
    if (p.x < 0.0 || p.y < 0.0 || p.x > 1.0 || p.y > 1.0) {
        fragColor = vec4(0.0);
        return;
    }
    vec2 t = vec2(textureSize(InSampler, 0));
    vec2 uv = (floor((A.xy + p * A.zw) * t) + 0.5) / t;
    vec4 c = textureLod(InSampler, uv, 0.0);
    if (c.a < 0.1) {
        fragColor = vec4(0.0);
        return;
    }
    fragColor = flame(InSampler, uv, c.rgb / c.a);
}
