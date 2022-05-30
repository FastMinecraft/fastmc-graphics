const float angleMultiplier = 1.57079637050628662109375;

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