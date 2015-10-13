package cn.momia.service.course.web.ctrl;

import cn.momia.api.course.dto.SubjectDto;
import cn.momia.api.course.dto.SubjectSkuDto;
import cn.momia.common.api.http.MomiaHttpResponse;
import cn.momia.service.course.subject.Subject;
import cn.momia.service.course.subject.SubjectService;
import cn.momia.service.course.subject.SubjectSku;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/subject")
public class SubjectController {
    @Autowired private SubjectService subjectService;

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public MomiaHttpResponse get(@PathVariable long id) {
        return MomiaHttpResponse.SUCCESS(buildSubjectDto(subjectService.get(id)));
    }

    private SubjectDto buildSubjectDto(Subject subject) {
        SubjectDto subjectDto = new SubjectDto();
        if (subject.exists()) {
            subjectDto.setId(subject.getId());
            subjectDto.setTitle(subject.getTitle());

            SubjectSku minPriceSku = subject.getMinPriceSku();
            subjectDto.setPrice(minPriceSku.getPrice());
            subjectDto.setOriginalPrice(minPriceSku.getOriginalPrice());

            subjectDto.setAge(subject.getAge());
            subjectDto.setJoined(subject.getJoined());
            subjectDto.setIntro(subject.getIntro());
            subjectDto.setNotice(subject.getNotice());
            subjectDto.setImgs(subject.getImgs());
        }

        return subjectDto;
    }

    @RequestMapping(value = "/{id}/sku", method = RequestMethod.GET)
    public MomiaHttpResponse listSkus(@PathVariable long id) {
        return MomiaHttpResponse.SUCCESS(buildSubjectSkuDtos(subjectService.listSkus(id)));
    }

    private List<SubjectSkuDto> buildSubjectSkuDtos(List<SubjectSku> skus) {
        List<SubjectSkuDto> skuDtos = new ArrayList<SubjectSkuDto>();
        for (SubjectSku sku : skus) {
            SubjectSkuDto skuDto = new SubjectSkuDto();
            skuDto.setId(sku.getId());
            skuDto.setSubjectId(sku.getSubjectId());
            skuDto.setPrice(sku.getPrice());
            skuDto.setDesc(sku.getDesc());

            skuDtos.add(skuDto);
        }

        return skuDtos;
    }
}
