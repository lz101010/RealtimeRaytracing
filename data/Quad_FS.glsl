#version 150
uniform sampler2D image;
in vec2 fs_in_tex;
out vec4 fs_out_color;

void main() {
    fs_out_color = texture(image, fs_in_tex);
}