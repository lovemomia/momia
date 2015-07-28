package cn.momia.service.product.place;

import cn.momia.common.service.Service;

import java.util.Collection;
import java.util.Map;

public interface PlaceService extends Service {
    Place get(long id);
    Map<Long, Place> get(Collection<Long> ids);
}
