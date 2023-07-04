#version 460
#include "/assets/shaders/util/Mat3Rotation.glsl"

layout(std140) uniform Global {
    mat4 projection;
    mat4 modelView;
    mat4 inverseProjection;
    mat4 inverseModelView;
    vec2 screenResolution;
    float partialTicks;
};

uniform vec3 offset;

layout(location = 0) in vec3 modelPosition;
layout(location = 1) in vec2 vertUV;
layout(location = 2) in vec3 vertNormal;
layout(location = 3) in int vertGroupID;


layout(location = 4) in vec3 renderPosition;
layout(location = 5) in vec2 vertLightMapUV;

layout(location = 6) in int rotationY;
layout(location = 7) in int rotationX;
layout(location = 8) in int colorIndex;
layout(location = 9) in float prevProgress;
layout(location = 10) in float progress;

out vec2 uv;
flat out vec3 normal;
out vec2 lightMapUV;

const float lidAngleMultiplier = 4.7123889803846898576939650749193;
const vec2 uvMultiplier = vec2(0.25, 0.125);

void main() {
    vec3 position = modelPosition * 0.9995;
    position.y += 0.001;
    normal = vertNormal;

    if (vertGroupID != 0) {
        float renderProgress = mix(prevProgress, progress, partialTicks);

        mat3 lidMatrix = mat3(1.0);
        lidMatrix = rotateY90(lidMatrix, renderProgress * 3.0);

        position = position * lidMatrix;
        position.y += renderProgress * 0.5;

        normal = normal * lidMatrix;
    }

    mat3 rotationMatrix = mat3(1.0);
    rotationMatrix = rotateX90(rotationMatrix, rotationX);
    rotationMatrix = rotateY90(rotationMatrix, rotationY);

    position.y -= 0.5;
    position = position * rotationMatrix;
    position.y += 0.5;

    gl_Position = projection * modelView * vec4(position + (renderPosition + offset), 1.0);
    uv = (vertUV + vec2(float(colorIndex % 4), float(colorIndex / 4))) * uvMultiplier;
    normal = normal * rotationMatrix;
    lightMapUV = vertLightMapUV * 0.99609375 + 0.03125;
}