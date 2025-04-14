// sse.ts
import { EventSourcePolyfill } from 'event-source-polyfill';

interface SSEOptions {
  onMessage?: (data: any) => void;
  onError?: (error: any) => void;
  onOpen?: () => void;
}

export function createSSEConnection(url: string, token: string, options?: SSEOptions) {
  const eventSource = new EventSourcePolyfill(url, {
    headers: {
      'Authorization': `Bearer ${token}`,
      'Accept': 'text/event-stream'
    },
    withCredentials: true
  });

  // 连接成功回调
  eventSource.addEventListener('open', () => {
    console.log('SSE连接成功');
    options?.onOpen?.();
  });

  // 接收消息回调
  eventSource.addEventListener('message', (event: MessageEvent) => {
    try {
      const data = JSON.parse(event.data);
      options?.onMessage?.(data);
    } catch (error) {
      console.error('解析消息失败:', error);
      options?.onMessage?.(event.data);
    }
  });

  // 错误处理
  eventSource.addEventListener('error', (error: Event) => {
    console.error('SSE连接错误:', error);
    options?.onError?.(error);
  });

  return {
    eventSource,
    close: () => {
      eventSource.close();
    }
  };
}