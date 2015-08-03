package cn.momia.service.web.ctrl.common;

import cn.momia.service.web.response.ResponseMessage;
import cn.momia.service.web.ctrl.AbstractController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/city")
public class CityController extends AbstractController {
    @RequestMapping(method = RequestMethod.GET)
    public ResponseMessage getAllCities() {
        return ResponseMessage.SUCCESS(commonServiceFacade.getAllCities());
    }
}
