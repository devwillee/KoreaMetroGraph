package io.github.devwillee.koreametrograph.api;

import java.util.concurrent.ConcurrentHashMap;

public abstract class MetroGraphFactory {
    private static final ConcurrentHashMap<String, MetroGraph> instanceManager = new ConcurrentHashMap<>();

    public static <T extends MetroGraphFactory> MetroGraph create(Class<T> clazz) {
        MetroGraph instance = null;

        try {
            if(instanceManager.containsKey(clazz.getName())) {
                instance = instanceManager.get(clazz.getName());
            }
            else {
                MetroGraphFactory factory = clazz.newInstance();
                instance = factory.adjust(factory.create(new MetroGraph()));
                instanceManager.put(clazz.getName(), instance);
            }
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }

        return instance;
    }

    /**
     * Graph를 생성한다. 단, 이어지지 않은 Edge가 포함된다.
     * @param metroGraph
     * @return Edge를 제대로 조정하지 않은 Graph
     */
    public abstract MetroGraph create(MetroGraph metroGraph);

    /**
     * 예외처리. 이전/다음역이 여러개 인 경우 처리와 이어지지 않은 노선에 대한 삭제. create 호출 이후 호출 된다.
     * @param metroGraph
     * @return 조정된 graph
     */
    public abstract MetroGraph adjust(MetroGraph metroGraph);
}
