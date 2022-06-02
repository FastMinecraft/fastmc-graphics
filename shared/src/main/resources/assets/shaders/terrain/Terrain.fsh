#version 460

layout(std140) uniform FogParameters {
    vec3 color;
    vec2 densityRange;
} fogParameters;

uniform sampler2D blockTexture;
uniform sampler2D lightMapTexture;

in FragData {
    vec3 color;
    vec2 uv;
    vec2 lightMapUV;
    float fogAmount;
} fragData;

out vec4 fragColor;

void main() {
    fragColor = texture2D(blockTexture, fragData.uv);
    #ifdef ALPHA_TEST
    if (fragColor.a <= 0.5) discard;
    #endif
    fragColor.rgb = mix(fogParameters.color, fragColor.rgb * fragData.color * texture2D(lightMapTexture, fragData.lightMapUV).rgb, fragData.fogAmount);
}