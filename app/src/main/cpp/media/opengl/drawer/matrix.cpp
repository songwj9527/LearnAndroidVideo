//
// Created by fgrid on 2021/7/21.
//

#include "matrix.h"
#include <math.h>
#include <string.h>

void gmVec3Normalize(gmVector3* out, gmVector3* in)
{
    GLfloat f = in->x * in->x + in->y * in->y + in->z * in->z;
    f = 1.0f / sqrt(f);

    out->x = in->x * f;
    out->y = in->y * f;
    out->z = in->z * f;
}

void gmVec3CrossProduct(gmVector3* out, gmVector3* v1, gmVector3* v2)
{
    gmVector3 result;

    result.x = v1->y * v2->z - v1->z * v2->y;
    result.y = v1->z * v2->x - v1->x * v2->z;
    result.z = v1->x * v2->y - v1->y * v2->x;

    *out = result;
}


//实现移动的矩阵操作
void gmMatrixTranslate(gmMatrix4* out, GLfloat x, GLfloat y, GLfloat z)
{
    if (out == NULL)
    {
        return;
    }

    out->m[ 0]=1.0f; out->m[ 4]=0.0f; out->m[ 8]=0.0f; out->m[12]=x;
    out->m[ 1]=0.0f; out->m[ 5]=1.0f; out->m[ 9]=0.0f; out->m[13]=y;
    out->m[ 2]=0.0f; out->m[ 6]=0.0f; out->m[10]=1.0f; out->m[14]=z;
    out->m[ 3]=0.0f; out->m[ 7]=0.0f; out->m[11]=0.0f; out->m[15]=1.0f;
}

//缩放的操作
void gmMatrixScale(gmMatrix4* out, GLfloat x, GLfloat y, GLfloat z)
{
    if (out == NULL)
    {
        return;
    }

    out->m[ 0]=x;        out->m[ 4]=0.0f; out->m[ 8]=0.0f; out->m[12]=0.0f;
    out->m[ 1]=0.0f; out->m[ 5]=y;        out->m[ 9]=0.0f; out->m[13]=0.0f;
    out->m[ 2]=0.0f; out->m[ 6]=0.0f; out->m[10]=z;        out->m[14]=0.0f;
    out->m[ 3]=0.0f; out->m[ 7]=0.0f; out->m[11]=0.0f; out->m[15]=1.0f;
}

//绕X旋转的操作
void gmMatrixRotateX(gmMatrix4* out, GLfloat x)
{
    if (out == NULL)
    {
        return;
    }

    GLfloat fcos, fsin;

    fcos = cos(x);
    fsin = sin(x);

    out->m[ 0]=1.0f;	out->m[ 4]=0.0f;	out->m[ 8]=0.0f;	out->m[12]=0.0f;
    out->m[ 1]=0.0f;	out->m[ 5]=fcos;	out->m[ 9]=fsin;	out->m[13]=0.0f;
    out->m[ 2]=0.0f;	out->m[ 6]=-fsin;	out->m[10]=fcos;	out->m[14]=0.0f;
    out->m[ 3]=0.0f;	out->m[ 7]=0.0f;	out->m[11]=0.0f;	out->m[15]=1.0f;
}
void gmMatrixRotateY(gmMatrix4* out, GLfloat y)
{
    if (out == NULL)
    {
        return;
    }

    GLfloat fcos, fsin;

    fcos = cos(y);
    fsin = sin(y);

    out->m[ 0]=fcos;     out->m[ 4]=0.0f; out->m[ 8]=-fsin;        out->m[12]=0.0f;
    out->m[ 1]=0.0f;     out->m[ 5]=1.0f; out->m[ 9]=0.0f;     out->m[13]=0.0f;
    out->m[ 2]=fsin;     out->m[ 6]=0.0f; out->m[10]=fcos;     out->m[14]=0.0f;
    out->m[ 3]=0.0f;     out->m[ 7]=0.0f; out->m[11]=0.0f;     out->m[15]=1.0f;
}



void gmMatrixRotateZ(gmMatrix4* out, GLfloat z)
{
    if (out == NULL)
    {
        return;
    }

    GLfloat fcos, fsin;

    fcos = cos(z);
    fsin = sin(z);

    out->m[ 0]=fcos;        out->m[ 4]=fsin;    out->m[ 8]=0.0f;    out->m[12]=0.0f;
    out->m[ 1]=-fsin;        out->m[ 5]=fcos;    out->m[ 9]=0.0f;    out->m[13]=0.0f;
    out->m[ 2]=0.0f;        out->m[ 6]=0.0f;    out->m[10]=1.0f;    out->m[14]=0.0f;
    out->m[ 3]=0.0f;        out->m[ 7]=0.0f;    out->m[11]=0.0f;    out->m[15]=1.0f;
}

void gmMatrix4Init(gmMatrix4* mat)
{
    if (mat == NULL)
    {
        return;
    }

    memset(mat, 0, sizeof(gmMatrix4));

    mat->m[0] = 1.0f;
    mat->m[1] = 1.0f;
    mat->m[2] = 1.0f;
    mat->m[3] = 1.0f;
}

void gmMatrixMultiply(gmMatrix4* out, gmMatrix4* m1, gmMatrix4* m2)
{
    if (out == NULL || m1 == NULL || m2 == NULL)
    {
        return;
    }

    out->m[ 0] = m1->m[ 0]*m2->m[ 0] + m1->m[ 1]*m2->m[ 4] + m1->m[ 2]*m2->m[ 8] + m1->m[ 3]*m2->m[12];
    out->m[ 1] = m1->m[ 0]*m2->m[ 1] + m1->m[ 1]*m2->m[ 5] + m1->m[ 2]*m2->m[ 9] + m1->m[ 3]*m2->m[13];
    out->m[ 2] = m1->m[ 0]*m2->m[ 2] + m1->m[ 1]*m2->m[ 6] + m1->m[ 2]*m2->m[10] + m1->m[ 3]*m2->m[14];
    out->m[ 3] = m1->m[ 0]*m2->m[ 3] + m1->m[ 1]*m2->m[ 7] + m1->m[ 2]*m2->m[11] + m1->m[ 3]*m2->m[15];

    out->m[ 4] = m1->m[ 4]*m2->m[ 0] + m1->m[ 5]*m2->m[ 4] + m1->m[ 6]*m2->m[ 8] + m1->m[ 7]*m2->m[12];
    out->m[ 5] = m1->m[ 4]*m2->m[ 1] + m1->m[ 5]*m2->m[ 5] + m1->m[ 6]*m2->m[ 9] + m1->m[ 7]*m2->m[13];
    out->m[ 6] = m1->m[ 4]*m2->m[ 2] + m1->m[ 5]*m2->m[ 6] + m1->m[ 6]*m2->m[10] + m1->m[ 7]*m2->m[14];
    out->m[ 7] = m1->m[ 4]*m2->m[ 3] + m1->m[ 5]*m2->m[ 7] + m1->m[ 6]*m2->m[11] + m1->m[ 7]*m2->m[15];

    out->m[ 8] = m1->m[ 8]*m2->m[ 0] + m1->m[ 9]*m2->m[ 4] + m1->m[10]*m2->m[ 8] + m1->m[11]*m2->m[12];
    out->m[ 9] = m1->m[ 8]*m2->m[ 1] + m1->m[ 9]*m2->m[ 5] + m1->m[10]*m2->m[ 9] + m1->m[11]*m2->m[13];
    out->m[10] = m1->m[ 8]*m2->m[ 2] + m1->m[ 9]*m2->m[ 6] + m1->m[10]*m2->m[10] + m1->m[11]*m2->m[14];
    out->m[11] = m1->m[ 8]*m2->m[ 3] + m1->m[ 9]*m2->m[ 7] + m1->m[10]*m2->m[11] + m1->m[11]*m2->m[15];

    out->m[12] = m1->m[12]*m2->m[ 0] + m1->m[13]*m2->m[ 4] + m1->m[14]*m2->m[ 8] + m1->m[15]*m2->m[12];
    out->m[13] = m1->m[12]*m2->m[ 1] + m1->m[13]*m2->m[ 5] + m1->m[14]*m2->m[ 9] + m1->m[15]*m2->m[13];
    out->m[14] = m1->m[12]*m2->m[ 2] + m1->m[13]*m2->m[ 6] + m1->m[14]*m2->m[10] + m1->m[15]*m2->m[14];
    out->m[15] = m1->m[12]*m2->m[ 3] + m1->m[13]*m2->m[ 7] + m1->m[14]*m2->m[11] + m1->m[15]*m2->m[15];
}


void gmMatrixOrthoM(gmMatrix4* out, gmVector4 *area, gmVector2 *deep) {
    if (area->x == area->y) {
        return;
    }
    if (area->w == area->z) {
        return;
    }
    if (deep->x == deep->y) {
        return;
    }

    const float r_width  = 1.0f / (area->y - area->x); // right - left
    const float r_height = 1.0f / (area->w - area->z); // top - bottom
    const float r_depth  = 1.0f / (deep->y - deep->x); // far - near
    const float x =  2.0f * (r_width);
    const float y =  2.0f * (r_height);
    const float z = -2.0f * (r_depth);
    const float tx = -(area->y + area->x) * r_width;
    const float ty = -(area->w + area->z) * r_height;
    const float tz = -(deep->y + deep->x) * r_depth;
    out->m[0] = x;
    out->m[5] = y;
    out->m[10] = z;
    out->m[12] = tx;
    out->m[13] = ty;
    out->m[14] = tz;
    out->m[15] = 1.0f;
    out->m[1] = 0.0f;
    out->m[2] = 0.0f;
    out->m[3] = 0.0f;
    out->m[4] = 0.0f;
    out->m[6] = 0.0f;
    out->m[7] = 0.0f;
    out->m[8] = 0.0f;
    out->m[9] = 0.0f;
    out->m[11] = 0.0f;
}

void gmMatrixLookAtLH(gmMatrix4* out, gmVector3* eye, gmVector3* at, gmVector3* up)
{
    gmVector3 f, s, u;
    gmMatrix4 t;

    f.x = eye->x - at->x;
    f.y = eye->y - at->y;
    f.z = eye->z - at->z;

    //取最小值
    gmVec3Normalize(&f, &f);
    gmVec3CrossProduct(&s, &f, up);
    gmVec3Normalize(&s, &s);
    gmVec3CrossProduct(&u, &s, &f);
    gmVec3Normalize(&u, &u);

    out->m[ 0] = s.x;
    out->m[ 1] = u.x;
    out->m[ 2] = -f.x;
    out->m[ 3] = 0;

    out->m[ 4] = s.y;
    out->m[ 5] = u.y;
    out->m[ 6] = -f.y;
    out->m[ 7] = 0;

    out->m[ 8] = s.z;
    out->m[ 9] = u.z;
    out->m[10] = -f.z;
    out->m[11] = 0;

    out->m[12] = 0;
    out->m[13] = 0;
    out->m[14] = 0;
    out->m[15] = 1;

    gmMatrixTranslate(&t, -eye->x, -eye->y, -eye->z);
    gmMatrixMultiply(out, &t, out);
}

void gmMatrix4IdentityM(gmMatrix4* mat) {
    for (int i=0 ; i<16 ; i++) {
        mat->m[i] = 0;
    }
    for(int i = 0; i < 16; i += 5) {
        mat->m[i] = 1.0f;
    }
}

void gmMatrixPerspectiveFovLH(gmMatrix4* out, GLfloat foy, GLfloat aspect, GLfloat near, GLfloat far)
{
    GLfloat f, n;

    f = 1.0f / (GLfloat)tan(foy * 0.5f);
    n = 1.0f / (far - near);

    out->m[ 0] = f / aspect;
    out->m[ 1] = 0;
    out->m[ 2] = 0;
    out->m[ 3] = 0;

    out->m[ 4] = 0;
    out->m[ 5] = f;
    out->m[ 6] = 0;
    out->m[ 7] = 0;

    out->m[ 8] = 0;
    out->m[ 9] = 0;
    out->m[10] = far * n;
    out->m[11] = 1;

    out->m[12] = 0;
    out->m[13] = 0;
    out->m[14] = -far * near * n;
    out->m[15] = 0;
}