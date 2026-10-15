#version 330

#moj_import <fireglow:lib.glsl>

uniform sampler2D Sampler0;

in vec2 uv;
in float keep;

out vec4 fragColor;

void main() {
    vec4 c = texture(Sampler0, uv);
    if (c.a < 0.1 || keep < -1.0) {
        discard;
    }
    fragColor = flame(Sampler0, uv, c.rgb);
}
