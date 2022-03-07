#version 330
precision highp float;

uniform mat4 projection;
uniform mat4 modelView;
uniform vec3 offset;

uniform float alpha;
uniform float partialTicks;

layout(location = 0) in vec3 modelPosition;
layout(location = 1) in vec2 vertUV;
layout(location = 2) in vec3 vertNormal;
layout(location = 3) in int vertGroupID;


layout(location = 4) in vec3 renderPosition;
layout(location = 5) in vec2 vertLightMapUV;

layout(location = 6) in int rotationY;
layout(location = 7) in int colorIndex;
layout(location = 8) in int isHead;

out vec2 uv;
flat out vec3 normal;
out vec2 lightMapUV;

#import /assets/shaders/util/Mat3Rotation.glsl

void main() {
    if (isHead == vertGroupID / 3) {
        gl_Position = vec4(0.0, 0.0, 0.0, 0.0);
        return;
    }

    mat3 rotationMatrix = mat3(1.0);
    rotationMatrix = rotateY90(rotationMatrix, rotationY);

    gl_Position = projection * modelView * vec4(modelPosition * rotationMatrix + (renderPosition + offset), 1.0);
    uv = vertUV * 0.25 + vec2(float(colorIndex % 4), float(colorIndex / 4)) * 0.25;
    normal = vertNormal * rotationMatrix;
    lightMapUV = vertLightMapUV * 0.99609375 + 0.03125;
}