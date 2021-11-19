#version 330
precision highp float;

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

const float angleMultiplier = 1.57079637050628662109375;
const float pi = 3.14159265358979323846;
const float toRadian = 0.01745329251994329576923690768489;

const vec3 headRotationPoint = vec3(0.0, 1.25, 0.5);

mat3 rotateX(mat3 matrix, float angle) {
    float sin = sin(angle);
    float cos = cos(angle);

    float rm11 = cos;
    float rm21 = -sin;
    float rm12 = sin;
    float rm22 = cos;

    float nm10 = matrix[1][0] * rm11 + matrix[2][0] * rm12;
    float nm11 = matrix[1][1] * rm11 + matrix[2][1] * rm12;
    float nm12 = matrix[1][2] * rm11 + matrix[2][2] * rm12;

    matrix[2][0] = matrix[1][0] * rm21 + matrix[2][0] * rm22;
    matrix[2][1] = matrix[1][1] * rm21 + matrix[2][1] * rm22;
    matrix[2][2] = matrix[1][2] * rm21 + matrix[2][2] * rm22;

    matrix[1][0] = nm10;
    matrix[1][1] = nm11;
    matrix[1][2] = nm12;

    return matrix;
}

mat3 rotateY(mat3 matrix, float angle) {
    float sin = sin(angle);
    float cos = cos(angle);

    float rm00 = cos;
    float rm20 = sin;
    float rm02 = -sin;
    float rm22 = cos;

    float nm00 = matrix[0][0] * rm00 + matrix[2][0] * rm02;
    float nm01 = matrix[0][1] * rm00 + matrix[2][1] * rm02;
    float nm02 = matrix[0][2] * rm00 + matrix[2][2] * rm02;

    matrix[2][0] = matrix[0][0] * rm20 + matrix[2][0] * rm22;
    matrix[2][1] = matrix[0][1] * rm20 + matrix[2][1] * rm22;
    matrix[2][2] = matrix[0][2] * rm20 + matrix[2][2] * rm22;

    matrix[0][0] = nm00;
    matrix[0][1] = nm01;
    matrix[0][2] = nm02;

    return matrix;
}

mat3 rotateZ(mat3 matrix, float angle) {
    float sin = sin(angle);
    float cos = cos(angle);

    float rm00 = cos;
    float rm10 = -sin;
    float rm01 = sin;
    float rm11 = cos;

    float nm00 = matrix[0][0] * rm00 + matrix[1][0] * rm01;
    float nm01 = matrix[0][1] * rm00 + matrix[1][1] * rm01;
    float nm02 = matrix[0][2] * rm00 + matrix[1][2] * rm01;

    matrix[1][0] = matrix[0][0] * rm10 + matrix[1][0] * rm11;
    matrix[1][1] = matrix[0][1] * rm10 + matrix[1][1] * rm11;
    matrix[1][2] = matrix[0][2] * rm10 + matrix[1][2] * rm11;

    matrix[0][0] = nm00;
    matrix[0][1] = nm01;
    matrix[0][2] = nm02;

    return matrix;
}

mat3 rotateX90(mat3 matrix, int angle) {
    return rotateX(matrix, angle * angleMultiplier);
}

mat3 rotateY90(mat3 matrix, int angle) {
    return rotateY(matrix, angle * angleMultiplier);
}

mat3 rotateZ90(mat3 matrix, int angle) {
    return rotateZ(matrix, angle * angleMultiplier);
}

mat3 rotateX90(mat3 matrix, float angle) {
    return rotateX(matrix, angle * angleMultiplier);
}

mat3 rotateY90(mat3 matrix, float angle) {
    return rotateY(matrix, angle * angleMultiplier);
}

mat3 rotateZ90(mat3 matrix, float angle) {
    return rotateZ(matrix, angle * angleMultiplier);
}

void main() {
    vec3 renderPosition = mix(prevEntityPos, entityPos, partialTicks);
    vec3 renderRotations = mix(prevRotations, rotations, partialTicks);
    vec2 renderLimbSwing = mix(prevLimbSwing, limbSwing, partialTicks);
    
    vec3 position = modelPosition;
    
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
            position += headRotationPoint;
            break;
        case 2:
        case 3:
            break;
        default:
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

            position -= legOffset;
            position *= rotateX(mat3(1.0), legAngle);
            position += legOffset;
    }

    gl_Position = projection * modelView * vec4(position * rotationMatrix + (renderPosition + offset), 1.0);

    uv = modelUV;
    normal = modelNormal * rotationMatrix;
    lightMapUV = vertLightMapUV * 0.99609375 + 0.03125;
}