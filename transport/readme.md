
# 一、netty的io模型

## 1.1、oio
oio：为同步阻塞io bio，netty已经废弃

客户端发起请求时，会一直阻塞当前线程，直到获取结果

## 1.2、nio
nio：同步非阻塞io，netty默认采用的就是此种方式
 
客户端发起请求，不会阻塞当前线程，当客户端线程在get获取结果的时候，
会从操作系统接收缓冲区中获取请求结果，如果未获取到的话，则阻塞当前线程直到可以获取结果。

## 1.3、aio

aio：异步非阻塞io，目前netty也未采用aio。aio模型也就是proactor模型，目前操作系统支持的还不太好。

客户端发起请求，不会阻塞当前线程，当操作系统接收缓冲区中有结果后，会执行客户端的回调函数，执行后的逻辑。

# 二、netty的请求处理模型

## 2.1、三种请求处理模型

单reactor单线程

单reactor多线程

多reactor多线程


## 2.2、netty中reactor模型的实现

netty-transport 是tansport的公共实现部分


netty-transport-native-epoll  是基于linux系统实现reactor模型   

netty-transport-native-kqueue 是基于mac系统实现reactor模型

netty-transport-native-unix-common 基于unix系统的公共部分



