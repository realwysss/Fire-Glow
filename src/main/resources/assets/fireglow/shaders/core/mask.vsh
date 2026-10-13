#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;

out vec2 uv;
out float keep;

void main() {
    vec4 v = ModelViewMat * vec4(Position, 1.0);
    gl_Position = ProjMat * vec4(v.xyz * 0.998, v.w);
    uv = UV0;
    keep = Color.a + step(1.0e30, abs(Normal.x) + float(abs(UV1.x) + abs(UV2.y)));
}
