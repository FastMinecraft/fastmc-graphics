#version 330

uniform sampler2D blockTexture;
uniform sampler2D lightMapTexture;
uniform float alphaTest;
uniform vec3 fogColor;

in vec4 color;
in vec2 uv;
in vec2 lightMapUV;
in float fogAmount;

out vec4 fragColor;

void main() {
    fragColor = color * texture2D(blockTexture, uv);
    if (fragColor.a <= alphaTest)discard;
    fragColor.rgb *= texture2D(lightMapTexture, lightMapUV).rgb;
    fragColor.rgb = mix(fogColor, fragColor.rgb, fogAmount);
}