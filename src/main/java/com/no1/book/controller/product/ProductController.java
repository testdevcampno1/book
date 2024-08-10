package com.no1.book.controller.product;

import com.no1.book.domain.product.CategoryDto;
import com.no1.book.domain.product.PageHandler;
import com.no1.book.domain.product.ProductDto;
import com.no1.book.service.product.CategoryService;
import com.no1.book.service.product.ProductService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
1. 리스트부터 띄워 보자 v
2. 띄운 리스트를 선택한 기준에 따라 정렬하도록 해보자 v
3. 선택한 카테고리에 대해 필터링 되도록 해보자 (필터링 된 리스트가 앞서 구현한 정렬 기능과 호환이 되어야 한다)

 */

@Controller
@RequestMapping("/product")
public class ProductController {
    @Autowired
    ProductService productService;

    @Autowired
    CategoryService categoryService;

    @GetMapping("/list")
    public String list(Integer page, Integer pageSize, String sortKey, String sortOrder, String cateKey, Model m) {

        if (page==null) page=1;
        if (pageSize==null) pageSize=10;
        if (sortKey == null) sortKey = "date";
        if (sortOrder == null) sortOrder = "desc";
        if (cateKey == null) cateKey = "010101";


        try {
            int totalCnt = productService.getProductCount();
            PageHandler pageHandler = new PageHandler(totalCnt, page, pageSize);

            Map map = new HashMap();
            map.put("offset", (page - 1) * pageSize);
            map.put("pageSize", pageSize);
            map.put("sortKey", sortKey);
            map.put("sortOrder", sortOrder);
            map.put("cateKey", cateKey + "%");

            List<ProductDto> prodList = productService.getSortedPage(map);
            List<CategoryDto> cateList = categoryService.getAllCategories();

            m.addAttribute("prodList", prodList);
            m.addAttribute("cateList", cateList);
            m.addAttribute("ph", pageHandler);
            m.addAttribute("pageSize", pageSize);
            m.addAttribute("sortKey", sortKey);
            m.addAttribute("sortOrder", sortOrder);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return "productList";
    }
}