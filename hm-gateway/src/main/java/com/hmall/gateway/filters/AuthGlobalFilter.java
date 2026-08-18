package com.hmall.gateway.filters;

import com.hmall.gateway.config.AuthProperties;
import com.hmall.gateway.util.JwtTool;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AuthGlobalFilter implements GlobalFilter, Ordered {
    private final AuthProperties authProperties;
    private final JwtTool jwtTool;

    private final AntPathMatcher antPathMatcher = new AntPathMatcher();//匹配路径
    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        //1.获取request
        ServerHttpRequest request = exchange.getRequest();
        //2.判断是否需要做登录拦截
        if(isExclude(request.getPath().toString())){
            //放行
            return chain.filter(exchange);
        }


        //3.获取Token
        String token=null;
        List<String> headers = request.getHeaders().get("authorization");
        if(headers!=null&&!headers.isEmpty()){
            token=headers.get(0);
        }

        //4.校验并解析Token
        Long userId=null;
        try {
            userId = jwtTool.parseToken(token);
        } catch (Exception e) {
            //拦截，设置响应状态码401
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
        //5.传递用户信息，把获取的用户写到请求头中
        //System.out.println("userId="+userId);
        String userInfo=userId.toString();
        ServerWebExchange swe = exchange.mutate()
                .request(builder -> builder.header("user-info", userInfo))
                .build();//生成一个新的exchange

        //6.放行
        return chain.filter(swe);


    }

    private boolean isExclude(String path) {
        for(String pathPattern:authProperties.getExcludePaths()){
            if(antPathMatcher.match(pathPattern,path)){
                return true;
            }
        }
        return false;
    }
}
