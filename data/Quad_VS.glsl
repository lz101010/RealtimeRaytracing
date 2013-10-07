#version 150
in vec4 vs_in_data;
out vec2 fs_in_tex;

void main() {
    fs_in_tex = vs_in_data.zw;
    vec2 pos = vs_in_data.xy;
    gl_Position = vec4(pos, 0, 1);
}