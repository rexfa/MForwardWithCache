# MForwardWithCache
这是2013年在某动最后的工程，现在应该是已经淘汰了，但是这时候引入了Java NIO的完全实现。这里保存下。

主要用途是实现和上级数据服务器的连接，然后转发到下级几个数据接收方，其中规定几个接收方，如果没有及时连接，本程序将歹为保存，等到接收方链接上来一并发出。强调性能。

后来自作主张完成了一个可以管理和查看状态的客户端，也用Java为了保证Linux下X11可以使用。

协议单独实现的包含在工程之内。
