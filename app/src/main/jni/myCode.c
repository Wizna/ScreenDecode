//
// Created by Ruiming Huang on 2016/5/27.
//

#include "com_example_testdecode_RSDecoder.h"
#include <string.h>
#include <jni.h>

#include <stdio.h>
#include <stdlib.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>

#define MM 	4
#define NN  ((1 << MM) - 1)
#define TT  2
#define KK  (NN - 2 * TT)
#define RSCODE_CODE_NUM_PER_GROUP 12
#define RSCODE_GROUP_NUM 2
#define BITS_PER_POS 2
#define TYPES_PER_CODE 3
#define GRID_WIDTH 3
#define GRID_HEIGHT 3
#define DATA_ROW 7
#define DATA_COL 7
#define CM_PER_INCH 2.54
#define BLACK_DOT (0xFF000000)
#define WHITE_DOT (0xFFFFFFFF)
#define GRID_SIZE ((GRID_WIDTH) * (GRID_HEIGHT))
#define GRID_PIXEL_WIDTH ((GRID_WIDTH) * (PIXEL_PER_DOT_WIDTH))
#define GRID_PIXEL_HEIGHT ((GRID_HEIGHT) * (PIXEL_PER_DOT_HEIGHT))
#define DATA_LEN ((DATA_ROW) * (DATA_COL))
#define FIELD_ROW (DATA_ROW + 2)
#define FIELD_COL (DATA_COL + 2)
#define CODE_NUM (1 << BITS_PER_POS)
#define PIXEL_PER_DOT_WIDTH 2
#define PIXEL_PER_DOT_HEIGHT 2
#define PIXEL_PER_DOT ((PIXEL_PER_DOT_WIDTH) * (PIXEL_PER_DOT_HEIGHT))
#define BLANK_PIXEL_HEIGHT 3
#define BLANK_PIXEL_WIDTH 3
#define VERT_BLANK_PIXEL_HEIGHT 3
#define FIELD_PIXEL_WIDTH (((FIELD_COL) * (GRID_PIXEL_WIDTH)) + ((FIELD_COL) * (BLANK_PIXEL_WIDTH)))
#define FIELD_PIXEL_HEIGHT (((FIELD_ROW) * (GRID_PIXEL_HEIGHT)) + ((FIELD_ROW) * (VERT_BLANK_PIXEL_HEIGHT)))
#define ACTUAL_FIELD_PIXEL_WIDTH (FIELD_PIXEL_WIDTH - GRID_PIXEL_WIDTH - BLANK_PIXEL_WIDTH)
#define ACTUAL_FIELD_PIXEL_HEIGHT (FIELD_PIXEL_HEIGHT - GRID_PIXEL_HEIGHT - BLANK_PIXEL_HEIGHT)

#define NORM 0
#define HORI_HALF 1
#define VERT_HALF 2
JNIEXPORT jint JNICALL Java_com_example_testdecode_RSDecoder_addInt
        (JNIEnv * a, jobject b , jint c, jint d){
    return c+d;
}



int alpha_to[NN+1], index_of[NN+1], gg[NN-KK+1] ;
int recd[NN], data[KK], bb[NN-KK] ;
int pp[MM+1] = {1,1,0,0,1};

int two_dimension_to_one(int row, int column, int column_length){
    return row * column_length + column;
}

int transfer_bits_per_pos(unsigned char *src, int src_len, int src_bits_per_code, int *data, int to_bits_per_code){
    int i,j,bit_len = src_len * src_bits_per_code,data_len = src_len * src_bits_per_code % to_bits_per_code == 0 ? src_len * src_bits_per_code / to_bits_per_code : (src_len * src_bits_per_code / to_bits_per_code) + 1,temp_data[data_len];
    unsigned char bitstring[bit_len];
    for (i = 0;i < src_len;i++){
        for (j = 0;j < src_bits_per_code;j++){
            bitstring[i * src_bits_per_code + j] = (src[i] >> (src_bits_per_code - 1 - j)) & 0x01;
        }
    }
    for (i = 0;i < data_len;i++){
        temp_data[i] = 0;
    }
    int overbound = 0;
    for (i = 0;i < data_len;i++){
        for (j = 0;j < to_bits_per_code;j++){
            if (i * to_bits_per_code + j >= bit_len){
                overbound = 1;
                break;
            }
            temp_data[i] |= (bitstring[i * to_bits_per_code + j]) << (to_bits_per_code - 1 - j);
        }
        if (overbound)
            break;
    }
    for (i = 0;i < data_len;i++){
        data[i] = temp_data[i];
    }
    return data_len;
}

/* 生成GF(2^m)空间 */
void generate_gf(){
    register int i, mask ;
    mask = 1 ;
    alpha_to[MM] = 0 ;
    for (i=0; i<MM; i++){
        alpha_to[i] = mask ;
        index_of[alpha_to[i]] = i ;
        if (pp[i]!=0)
            alpha_to[MM] ^= mask ;
        mask <<= 1 ;
    }
    index_of[alpha_to[MM]] = MM ;
    mask >>= 1 ;
    for (i=MM+1; i<NN; i++){
        if (alpha_to[i-1] >= mask)
            alpha_to[i] = alpha_to[MM] ^ ((alpha_to[i-1]^mask)<<1) ;
        else
            alpha_to[i] = alpha_to[i-1]<<1 ;
        index_of[alpha_to[i]] = i ;
    }
    index_of[0] = -1 ;
}

/* 生成g(x)生成多项式*/
void gen_poly(){
    register int i,j ;
    gg[0] = 2 ;    /* primitive element alpha = 2  for GF(2**MM)  */
    gg[1] = 1 ;    /* g(x) = (X+alpha) initially */
    for (i=2; i<=NN-KK; i++)
    {
        gg[i] = 1 ;
        for (j=i-1; j>0; j--)
            if (gg[j] != 0)
                gg[j] = gg[j-1]^ alpha_to[(index_of[gg[j]]+i)%NN];
            else
                gg[j] = gg[j-1] ;
        gg[0] = alpha_to[(index_of[gg[0]]+i)%NN] ; /* gg[0] can never be zero */
    }
    /* convert gg[] to index form for quicker encoding */
    for (i=0; i<=NN-KK; i++)
        gg[i] = index_of[gg[i]];
}

/* 编码 */
int *encode_rs(int *new_data){
    register int i,j ;
    int feedback ;
    for (i = 0;i < KK;i++){
        data[i] = new_data[i];
    }
    for (i=0; i<NN-KK; i++)
        bb[i] = 0 ;
    for (i=KK-1; i>=0; i--){
        //逐步的将下一步要减的，存入bb(i)
        feedback = index_of[data[i]^bb[NN-KK-1]] ;
        if(feedback != -1){
            for (j=NN-KK-1; j>0; j--){
                if (gg[j] != -1){
                    bb[j] = bb[j-1]^alpha_to[(gg[j]+feedback)%NN] ;		//plus = ^
                }
                else{
                    bb[j] = bb[j-1] ;
                }
            }
            bb[0] = alpha_to[(gg[0]+feedback)%NN] ;
        }
        else{
            for (j=NN-KK-1; j>0; j--)
                bb[j] = bb[j-1] ;
            bb[0] = 0 ;
        }
    }
    return &(bb[0]);
}

/* 解码 */
int *decode_rs(int *new_recd){
    register int i,j,u,q ;
    int elp[NN-KK+2][NN-KK], d[NN-KK+2], l[NN-KK+2], u_lu[NN-KK+2], s[NN-KK+1] ;
    int count=0, syn_error=0, root[TT], loc[TT], z[TT+1], err[NN], reg[TT+1] ;
    for (i = 0;i < NN;i++)
        recd[i] = new_recd[i];

    /* first form the syndromes */
    for(i=0; i<NN; i++)
        //转换成GF空间的alpha幂次
        if(recd[i] == -1)
            recd[i] = 0;
        else
            recd[i] = index_of[recd[i]];

    for (i=1; i<=NN-KK; i++){
        s[i] = 0 ;
        for (j=0; j<NN; j++)
            if (recd[j]!=-1)
                s[i] ^= alpha_to[(recd[j]+i*j)%NN] ;
        /* recd[j] in index form */
        /* convert syndrome from polynomial form to index form  */
        if (s[i]!=0)
            syn_error=1 ;        /* set flag if non-zero syndrome => error */
        s[i] = index_of[s[i]] ;
    }

    /* if errors, try and correct */
    if (syn_error){
        /* compute the error location polynomial via the Berlekamp iterative algorithm,
           following the terminology of Lin and Costello :   d[u] is the 'mu'th
           discrepancy, where u='mu'+1 and 'mu' (the Greek letter!) is the step number
           ranging from -1 to 2*TT (see L&C),  l[u] is the
           degree of the elp at that step, and u_l[u] is the difference between the
           step number and the degree of the elp.*/
        /* initialise table entries */
        d[0] = 0 ;           /* index form */
        d[1] = s[1] ;        /* index form */
        elp[0][0] = 0 ;      /* index form */
        elp[1][0] = 1 ;      /* polynomial form */
        for (i=1; i<NN-KK; i++)
        {
            elp[0][i] = -1 ;   /* index form */
            elp[1][i] = 0 ;   /* polynomial form */
        }
        l[0] = 0 ;
        l[1] = 0 ;
        u_lu[0] = -1 ;
        u_lu[1] = 0 ;
        u = 0 ;
        do{
            u++ ;
            if (d[u]==-1){
                l[u+1] = l[u] ;
                for (i=0; i<=l[u]; i++){
                    elp[u+1][i] = elp[u][i] ;
                    elp[u][i] = index_of[elp[u][i]] ;
                }
            }
            else{
                /* search for words with greatest u_lu[q] for which d[q]!=0 */
                q = u-1 ;
                while ((d[q]==-1) && (q>0))
                    q-- ;
                /* have found first non-zero d[q]  */
                if (q>0){
                    j=q ;
                    do{
                        j-- ;
                        if ((d[j]!=-1) && (u_lu[q]<u_lu[j]))
                            q = j ;
                    }while (j>0);
                }
                /* have now found q such that d[u]!=0 and u_lu[q] is maximum */
                /* store degree of new elp polynomial */
                if (l[u]>l[q]+u-q)
                    l[u+1] = l[u] ;
                else
                    l[u+1] = l[q]+u-q ;
                /* form new elp(x) */
                for (i=0; i<NN-KK; i++)
                    elp[u+1][i] = 0 ;
                for (i=0; i<=l[q]; i++)
                    if (elp[q][i]!=-1)
                        elp[u+1][i+u-q] = alpha_to[(d[u]+NN-d[q]+elp[q][i])%NN] ;
                for (i=0; i<=l[u]; i++){
                    elp[u+1][i] ^= elp[u][i] ;
                    elp[u][i] = index_of[elp[u][i]] ;  /*convert old elp value to index*/
                }
            }
            u_lu[u+1] = u-l[u+1] ;
            /* form (u+1)th discrepancy */
            if (u<NN-KK){
                /* no discrepancy computed on last iteration */
                if (s[u+1]!=-1)
                    d[u+1] = alpha_to[s[u+1]] ;
                else
                    d[u+1] = 0 ;
                for (i=1; i<=l[u+1]; i++)
                    if ((s[u+1-i]!=-1) && (elp[u+1][i]!=0))
                        d[u+1] ^= alpha_to[(s[u+1-i]+index_of[elp[u+1][i]])%NN] ;
                d[u+1] = index_of[d[u+1]] ;    /* put d[u+1] into index form */
            }
        } while ((u<NN-KK) && (l[u+1]<=TT)) ;
        u++ ;

        if (l[u]<=TT){
            /* can correct error */
            /* put elp into index form */
            for (i=0; i<=l[u]; i++)
                elp[u][i] = index_of[elp[u][i]] ;
            /* find roots of the error location polynomial */
            /*求错误位置多项式的根*/
            for (i=1; i<=l[u]; i++)
                reg[i] = elp[u][i] ;
            count = 0 ;
            for (i=1; i<=NN; i++){
                q = 1 ;
                for (j=1; j<=l[u]; j++)
                    if (reg[j]!=-1){
                        reg[j] = (reg[j]+j)%NN ;
                        q ^= alpha_to[reg[j]] ;
                    }
                /* store root and error location number indices */
                if (!q){
                    root[count] = i;
                    loc[count] = NN-i ;
                    count++ ;
                };
            }
            /* no. roots = degree of elp hence <= TT errors */
            if (count==l[u]){
                /* form polynomial z(x) */
                for (i=1; i<=l[u]; i++){
                    /* Z[0] = 1 always - do not need */
                    if ((s[i]!=-1) && (elp[u][i]!=-1))
                        z[i] = alpha_to[s[i]] ^ alpha_to[elp[u][i]] ;
                    else if ((s[i]!=-1) && (elp[u][i]==-1))
                        z[i] = alpha_to[s[i]] ;
                    else if ((s[i]==-1) && (elp[u][i]!=-1))
                        z[i] = alpha_to[elp[u][i]] ;
                    else
                        z[i] = 0 ;
                    for (j=1; j<i; j++)
                        if ((s[j]!=-1) && (elp[u][i-j]!=-1))
                            z[i] ^= alpha_to[(elp[u][i-j] + s[j])%NN] ;
                    z[i] = index_of[z[i]] ;         /* put into index form */
                } ;
                /* evaluate errors at locations given by error location numbers loc[i] */
                /*计算错误图样*/
                for (i=0; i<NN; i++){
                    err[i] = 0 ;
                    if (recd[i]!=-1)        /* convert recd[] to polynomial form */
                        recd[i] = alpha_to[recd[i]] ;
                    else
                        recd[i] = 0 ;
                }
                for (i=0; i<l[u]; i++){
                    /* compute numerator of error term first */
                    err[loc[i]] = 1;
                    /* accounts for z[0] */
                    for (j=1; j<=l[u]; j++)
                        if (z[j]!=-1)
                            err[loc[i]] ^= alpha_to[(z[j]+j*root[i])%NN] ;
                    if (err[loc[i]]!=0){
                        err[loc[i]] = index_of[err[loc[i]]] ;
                        q = 0 ;
                        /* form denominator of error term */
                        for (j=0; j<l[u]; j++)
                            if (j!=i)
                                q += index_of[1^alpha_to[(loc[j]+root[i])%NN]] ;
                        q = q % NN ;
                        err[loc[i]] = alpha_to[(err[loc[i]]-q+NN)%NN] ;
                        recd[loc[i]] ^= err[loc[i]] ;  /*recd[i] must be in polynomial form */
                    }
                }
            }
            else{
                /* no. roots != degree of elp => >TT errors and cannot solve */
                /*错误太多，无法更正*/
                for (i=0; i<NN; i++)        /* could return error flag if desired */
                    if (recd[i]!=-1)        /* convert recd[] to polynomial form*/
                        recd[i] = alpha_to[recd[i]] ;
                    else
                        recd[i] = 0 ;     /* just output received codeword as is */
                recd[0] = -1;
            }
        }
        else{
            /* elp has degree has degree >TT hence cannot solve */
            /*错误太多，无法更正*/
            for (i=0; i<NN; i++)       /* could return error flag if desired */
                if (recd[i]!=-1)        /* convert recd[] to polynomial form */
                    recd[i] = alpha_to[recd[i]] ;
                else
                    recd[i] = 0 ;     /* just output received codeword as is */
            recd[0] = -1;
        }
    }
    else{
        /* no non-zero syndromes => no errors: output received codeword */
        for (i=0; i<NN; i++){
            if (recd[i]!=-1)
                /* convert recd[] to polynomial form */
                recd[i] = alpha_to[recd[i]] ;
            else
                recd[i] = 0 ;
        }
    }
    return &(recd[0]);
}

/* initialization */
void rscode_init(){
    generate_gf();
    gen_poly();
}


JNIEXPORT jint JNICALL Java_com_example_testdecode_RSDecoder_decodeRS
        (JNIEnv *env, jobject b, jintArray inputArray, jintArray infoArray){
    jboolean check;
    int *input= (*env)->GetIntArrayElements(env,inputArray, &check);
    int *info_array= (*env)->GetIntArrayElements(env,infoArray, &check);

    int rscode_len = RSCODE_GROUP_NUM * RSCODE_CODE_NUM_PER_GROUP * MM / BITS_PER_POS,data_len;
    int i, j, data[DATA_LEN * BITS_PER_POS], rscode[DATA_LEN * BITS_PER_POS];
    unsigned char char_data[DATA_LEN];
    int type = -1;

    // transpose
    data[DATA_LEN - 1] = input[0];
    for (i = 0;i < DATA_ROW;i++){
        for (j = 0;j < DATA_COL;j++){
            if (i != DATA_ROW - 1 || j != DATA_COL - 1){
                if (j + 1 >= DATA_ROW){
                    int now = (j + 1) % DATA_ROW,count = (j + 1) / DATA_ROW;
                    data[two_dimension_to_one(i,j,DATA_COL)] = input[two_dimension_to_one(now, i + count, DATA_ROW)];
                }
                else{
                    data[two_dimension_to_one(i,j,DATA_COL)] = input[two_dimension_to_one(j + 1, i, DATA_ROW)];
                }
            }
        }
    }
    for (i = 0;i < DATA_LEN;i++){
        char_data[i] = data[i];
    }

    type = (char_data[DATA_LEN - 1] & 0x2) >> 1;
    data_len = transfer_bits_per_pos(char_data,rscode_len, BITS_PER_POS,data, MM);

    for (i = 0;i < RSCODE_GROUP_NUM;i++){
        for (j = 0;j < RSCODE_CODE_NUM_PER_GROUP;j++){
            char_data[i * NN + j] = data[i * RSCODE_CODE_NUM_PER_GROUP + j];
        }
        for (j = 0;j < NN - RSCODE_CODE_NUM_PER_GROUP;j++){
            char_data[i * NN + RSCODE_CODE_NUM_PER_GROUP + j] = 0;
        }
    }
    for (i = 0;i < RSCODE_GROUP_NUM * NN;i++){
        rscode[i] = char_data[i];
    }
    rscode_init();
    int *corrected_code;
    for (i = 0;i < RSCODE_GROUP_NUM;i++){
        corrected_code = decode_rs(rscode + i * NN);
        if (corrected_code[0] == -1){
            return -1;
        }

        for (j = 0;j < KK - (NN - RSCODE_CODE_NUM_PER_GROUP);j++){
            data[i * (KK - (NN - RSCODE_CODE_NUM_PER_GROUP)) + j] = corrected_code[NN - KK + j];
        }
    }
    for (i = 0;i < RSCODE_GROUP_NUM * (KK - (NN - RSCODE_CODE_NUM_PER_GROUP));i++){
        info_array[i] = data[i];
    }
    (*env)->ReleaseIntArrayElements(env, infoArray,info_array, 0);
    return type;
}