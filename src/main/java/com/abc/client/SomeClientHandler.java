/**
 * Author:   LiXiaoPeng
 * Date:     2019/9/29 12:21
 * Description:
 */
package com.abc.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.concurrent.ScheduledFuture;

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class SomeClientHandler extends ChannelInboundHandlerAdapter {

    private Bootstrap bootstrap;

    public SomeClientHandler(Bootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }

    // 当Channel 被激活后会触发该方法的执行（该方法就在连接成功后会执行一次）
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        randomSendHeartBeat(ctx.channel());
    }

    // 随机发送心跳
    private void randomSendHeartBeat(Channel channel) {
        //生成一个[1,8)的随机数，作为心跳间隔
        int heartBeatInternal = new Random().nextInt(7) + 1;
        System.out.println(heartBeatInternal + "秒后将再发送下一次心跳");
        ScheduledFuture<?> schedule = channel.eventLoop().schedule(() -> {
            if (channel.isActive()) {
                channel.writeAndFlush("PING~");
            } else {
                System.out.println("服务器已经断开");
                channel.closeFuture();
            }
        }, heartBeatInternal, TimeUnit.SECONDS);

        //未异步定时任务添加监听器
        schedule.addListener((future) -> {
            // 若定时任务执行成功，则重新再随机发送心跳
            if(future.isSuccess()) {
                randomSendHeartBeat(channel);
            }
        });
    }

    //只要CHANEL 被钝化（关闭）就会触发该方法的执行
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("服务端关闭, 重连机制开启~");
        ctx.channel().eventLoop().schedule(() -> {
            bootstrap.connect("localhost", 8888);
        }, 1, TimeUnit.SECONDS);

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
