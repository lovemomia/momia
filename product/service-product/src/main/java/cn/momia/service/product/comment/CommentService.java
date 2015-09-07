package cn.momia.service.product.comment;

import java.util.List;

public interface CommentService {
    long add(Comment comment);
    long queryCountByProduct(long productId);
    List<Comment> queryByProduct(long productId, int start, int count);
}
