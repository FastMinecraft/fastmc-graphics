#version 330
precision highp float;

uniform mat4 projection;
uniform mat4 modelView;
uniform float alpha;
uniform float partialTicks;

layout(location = 0) in vec3 modelPosition;
layout(location = 1) in vec2 vertUV;
layout(location = 2) in vec3 vertNormal;
layout(location = 3) in int vertGroupID;


layout(location = 4) in vec3 renderPosition;
layout(location = 5) in vec3 prevRenderPosition;
layout(location = 6) in vec2 vertLightMapUV;

layout(location = 7) in vec3 prevFinalRotation;
layout(location = 8) in vec3 finalRotation;
layout(location = 9) in vec4 headRotation;

out vec2 uv;
flat out vec3 normal;
out vec2 lightMapUV;

const vec3 headRotationPointOffset = vec3(0.0, 1.5, 0.0);

#import /assets/shaders/util/Mat3Rotation.glsl

void main() {
    vec3 position = modelPosition;
    normal = vertNormal;

    switch (vertGroupID) {
        // Head
        case 0:
        vec2 renderHeadRotation = mix(headRotation.xy, headRotation.zw, partialTicks);
        mat3 headRotationMatrix = mat3(1.0);

        headRotationMatrix = rotateX(headRotationMatrix, renderHeadRotation.y);
        headRotationMatrix = rotateY(headRotationMatrix, renderHeadRotation.x);

        position -= headRotationPointOffset;
        position = position * headRotationMatrix;
        position += headRotationPointOffset;

        normal = normal * headRotationMatrix;
        break;
        // Body
        case 1:

        break;
        // Left Arm
        case 2:

        break;
        // Right Arm
        case 3:

        break;
        // Left Leg
        case 4:

        break;
        // Right Leg
        default :

        break;
    }

    vec2 renderFinalRotation = mix(prevFinalRotation, finalRotation, partialTicks);
    mat3 rotationMatrix = mat3(1.0);
    rotationMatrix = rotateY(rotationMatrix, renderFinalRotation.y);

    vec3 interpolated = mix(prevRenderPosition, renderPosition, partialTicks);

    gl_Position = projection * modelView * vec4(position * rotationMatrix + interpolated, 1.0);
    uv = vertUV;
    normal = normal * rotationMatrix;
    lightMapUV = vertLightMapUV * 0.99609375 + 0.03125;
}