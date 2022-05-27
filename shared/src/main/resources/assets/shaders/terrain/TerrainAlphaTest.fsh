#version 460

uniform sampler2D blockTexture;
uniform sampler2D lightMapTexture;
uniform vec3 fogColor;

in vec3 color;
in vec2 uv;
in vec2 lightMapUV;
in float fogAmount;

out vec4 fragColor;

void main() {
    fragColor = texture2D(blockTexture, uv);
    if (fragColor.a <= 0.5) discard;
    fragColor.rgb = mix(fogColor, fragColor.rgb * color * texture2D(lightMapTexture, lightMapUV).rgb, fogAmount);
}