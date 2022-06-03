#version 460

layout(binding = 0) uniform sampler2D texture;
layout(binding = LIGHT_MAP_UNIT) uniform sampler2D lightMapTexture;

in vec2 uv;
flat in vec3 normal;
in vec2 lightMapUV;

out vec4 fragColor;

const vec3 lightPos1 = vec3(0.16169041, 0.80845207, -0.5659165);
const vec3 lightPos2 = vec3(-0.16169041, 0.80845207, 0.5659165);

float calcDiffuse(vec3 lightPos) {
    return max(dot(normal, lightPos), 0.0);
}

void main() {
    fragColor = texture2D(texture, uv);
    #ifdef ALPHA_TEST
    if (fragColor.a <= 0.5) discard;
    #endif

    vec3 lightColor = texture2D(lightMapTexture, lightMapUV).rgb;
    float diffuse = calcDiffuse(lightPos1) + calcDiffuse(lightPos2);
    float lightness = min(diffuse * 0.6 + 0.4, 1.0);

    fragColor.rgb *= lightness * lightColor;
}