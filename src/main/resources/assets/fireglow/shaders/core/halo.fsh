#version 330

uniform sampler2D Sampler0;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    vec4 c = texture(Sampler0, texCoord0) * vertexColor.a;
    if (c.a <= 0.0 && max(c.r, max(c.g, c.b)) <= 0.0) {
        discard;
    }
    fragColor = c;
}
