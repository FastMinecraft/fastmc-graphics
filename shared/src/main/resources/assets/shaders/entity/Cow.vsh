#version 460
#import /assets/shaders/util/Mat3Rotation.glsl

uniform mat4 projection;
uniform mat4 modelView;
uniform vec3 offset;

uniform float alpha;
uniform float partialTicks;

layout(location = 0) in vec3 modelPosition;
layout(location = 1) in vec2 modelUV;
layout(location = 2) in vec3 modelNormal;
layout(location = 3) in int modelGroup;


layout(location = 4) in vec3 prevEntityPos;
layout(location = 5) in vec3 entityPos;
layout(location = 6) in vec2 vertLightMapUV;

layout(location = 7) in vec3 prevRotations;
layout(location = 8) in vec3 rotations;

layout(location = 9) in vec2 prevLimbSwing;
layout(location = 10) in vec2 limbSwing;

out vec2 uv;
flat out vec3 normal;
out vec2 lightMapUV;

const float pi = 3.14159265358979323846;
const float toRadian = 0.01745329251994329576923690768489;
const vec3 headRotationPoint = vec3(0.0, 1.25, 0.5);

void main() {
    vec3 renderPosition = mix(prevEntityPos, entityPos, partialTicks);
    vec3 renderRotations = mix(prevRotations, rotations, partialTicks);
    vec2 renderLimbSwing = mix(prevLimbSwing, limbSwing, partialTicks);
    
    vec3 position = modelPosition;
    vec3 normal = modelNormal;
    
    mat3 rotationMatrix = mat3(1.0);
    rotationMatrix = rotateY(rotationMatrix, renderRotations.x * toRadian);
    
    switch(modelGroup) {
        case 0:
        case 1:
            mat3 headMatrix = mat3(1.0);
            headMatrix = rotateX(headMatrix, -renderRotations.z * toRadian);
            headMatrix = rotateY(headMatrix, (renderRotations.y - renderRotations.x) * toRadian);

            position -= headRotationPoint;
            position *= headMatrix;
            normal *= headMatrix;
            position += headRotationPoint;
            break;
        case 2:
        case 3:
            break;
        default:
            mat3 legMatrix = mat3(1.0);
            vec3 legOffset = vec3(0.0);
            float legAngle = 0.0;

            switch(modelGroup) {
                case 4:
                    legOffset = vec3(-0.25, 0.75, -0.4375);
                    legAngle = cos(renderLimbSwing.x * 0.6662 + pi) * 1.4 * renderLimbSwing.y;
                    break;
                case 5:
                    legOffset = vec3(0.25, 0.75, -0.4375);
                    legAngle = cos(renderLimbSwing.x * 0.6662) * 1.4 * renderLimbSwing.y;
                    break;
                case 6:
                    legOffset = vec3(-0.25, 0.75, 0.375);
                    legAngle = cos(renderLimbSwing.x * 0.6662) * 1.4 * renderLimbSwing.y;
                    break;
                case 7:
                    legOffset = vec3(0.25, 0.75, 0.375);
                    legAngle = cos(renderLimbSwing.x * 0.6662 + pi) * 1.4 * renderLimbSwing.y;
                    break;
                default:
                    break;
            }

            legMatrix = rotateX(legMatrix, legAngle);

            position -= legOffset;
            position *= legMatrix;
            normal *= headMatrix;
            position += legOffset;
    }

    gl_Position = projection * modelView * vec4(position * rotationMatrix + renderPosition, 1.0);

    uv = modelUV;
    normal = modelNormal * rotationMatrix;
    lightMapUV = vertLightMapUV * 0.99609375 + 0.03125;
}