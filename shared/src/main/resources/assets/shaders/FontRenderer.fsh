#version 460

uniform sampler2D glyphTexture;

in vec4 color;
in vec2 uv;

out vec4 fragColor;

void main() {
    float alpha = texture(glyphTexture, uv).r;
    if (alpha == 0.0) discard;
    fragColor = vec4(color.rgb, color.a * alpha);
}