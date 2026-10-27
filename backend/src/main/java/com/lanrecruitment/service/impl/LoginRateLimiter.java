package com.lanrecruitment.service.impl;

import com.lanrecruitment.common.enums.ErrorCode;
import com.lanrecruitment.exception.BizException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Single-instance local protection: five failed attempts per account and direct client per minute. */
@Component
public class LoginRateLimiter {
    private static final int MAX_KEYS=10000, MAX_FAILURES=5;
    private static final long WINDOW_MS=60000;
    private final Map<String, State> states=new LinkedHashMap<String, State>(16,0.75f,true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, State> eldest) { return size()>MAX_KEYS; }
    };
    private String key(String username) {
        ServletRequestAttributes attrs=(ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        String remote=attrs==null ? "unknown" : attrs.getRequest().getRemoteAddr();
        return remote+"\u0000"+username;
    }
    public synchronized void check(String username) {
        String key=key(username); State state=states.get(key); long now=System.currentTimeMillis();
        if (state!=null && now-state.start>=WINDOW_MS) { states.remove(key); state=null; }
        if (state!=null && state.failures>=MAX_FAILURES) throw new BizException(ErrorCode.AUTH_LOGIN_RATE_LIMITED);
    }
    public synchronized void recordFailure(String username) {
        String key=key(username); long now=System.currentTimeMillis(); State state=states.get(key);
        if (state==null || now-state.start>=WINDOW_MS) { state=new State(now); states.put(key,state); }
        state.failures++;
    }
    public synchronized void clear(String username) { states.remove(key(username)); }
    private static class State { final long start; int failures; State(long start) { this.start=start; } }
}
