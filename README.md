# CameraOpenCv 
camera2+opencv 
## 1.主要是对预览做灰度处理 COLOR_RGBA2GRAY
```
Mat src(h + h / 2, w, CV_8UC1, data);
    cvtColor(src, src, COLOR_YUV2RGBA_NV21);
    if (cameraId == 1) {
        rotate(src, src, ROTATE_90_COUNTERCLOCKWISE);
        flip(src, src, 1);
    } else {
        //顺时针旋转90度
        rotate(src, src, ROTATE_90_CLOCKWISE);
    }
//    灰色
    cvtColor(src, gray, COLOR_RGBA2GRAY);
    //增强对比度 (直方图均衡)
    equalizeHist(gray, gray);
    cvtColor(gray,rgba, COLOR_GRAY2RGBA);
 ```
## 2.拷贝数据到ANativeWindow
 ```
int srclineSize = rgba.cols*4 ;
        int dstlineSize = buffer.stride *4;
        for (int i = 0; i < buffer.height; ++i) {
            memcpy(dstData+i*dstlineSize, rgba.data+i*srclineSize, srclineSize);
}
 ```
## 这里面有一个坑，COLOR_RGBA2GRAY GRAY      
![image](https://user-images.githubusercontent.com/50398504/167818488-31b2e21b-b647-4cdb-8a47-ededf57a88ec.png)
这时候如果拷贝的话会有对齐问题，显示到界面上就会有乱码。
处理方式是cvtColor(gray,rgba, COLOR_GRAY2RGBA);又做了一遍gray-rgb的转换。
