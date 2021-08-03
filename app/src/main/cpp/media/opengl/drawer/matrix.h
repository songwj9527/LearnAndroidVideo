//
// Created by fgrid on 2021/7/21.
//

#ifndef OPENVIDEO_MATRIX_H
#define OPENVIDEO_MATRIX_H

extern "C" {
#include <GLES2/gl2.h>
};

typedef struct _gmVector3
{
    GLfloat x;
    GLfloat y;
    GLfloat z;
}gmVector3;

typedef struct _gmMatrix4
{
    GLfloat m[16];
}gmMatrix4;

typedef struct _gmVector2
{
    float x;
    float y;
}gmVector2;


typedef struct _gmVector4
{
    float x;
    float y;
    float z;
    float w;
}gmVector4;


void gmVec3Normalize(gmVector3* out, gmVector3* in);
void gmVec3CrossProduct(gmVector3* out, gmVector3* v1, gmVector3* v2);
//实现移动的矩阵操作
void gmMatrixTranslate(gmMatrix4* out, GLfloat x, GLfloat y, GLfloat z);
//缩放的操作
void gmMatrixScale(gmMatrix4* out, GLfloat x, GLfloat y, GLfloat z);
//绕X旋转的操作
void gmMatrixRotateX(gmMatrix4* out, GLfloat x);
void gmMatrixRotateY(gmMatrix4* out, GLfloat y);
void gmMatrixRotateZ(gmMatrix4* out, GLfloat z);
void gmMatrix4Init(gmMatrix4* mat);
void gmMatrixMultiply(gmMatrix4* out, gmMatrix4* m1, gmMatrix4* m2);
void gmMatrixOrthoM(gmMatrix4* out, gmVector4 *area, gmVector2 *deep);
void gmMatrixLookAtLH(gmMatrix4* out, gmVector3* eye, gmVector3* at, gmVector3* up);
void gmMatrix4IdentityM(gmMatrix4* mat);
void gmMatrixPerspectiveFovLH(gmMatrix4* out, GLfloat foy, GLfloat aspect, GLfloat near, GLfloat far);

#endif //OPENVIDEO_MATRIX_H
