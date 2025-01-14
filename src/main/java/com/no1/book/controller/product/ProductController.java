package com.no1.book.controller.product;

import com.no1.book.domain.product.AuthorDto;
import com.no1.book.domain.product.CategoryDto;
import com.no1.book.domain.product.CustomerProductDto;
import com.no1.book.domain.product.PageHandler;
import com.no1.book.domain.product.ProductDto;
import com.no1.book.domain.product.SearchCondition;
import com.no1.book.service.product.APIService;
import com.no1.book.service.product.AuthorService;
import com.no1.book.service.product.CategoryService;
import com.no1.book.service.product.FlaskService;
import com.no1.book.service.product.ProductService;
import com.no1.book.service.product.ReviewService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/product")
public class ProductController {
    @Autowired
    ProductService productService;

    @Autowired
    CategoryService categoryService;

    @Autowired
    AuthorService authorService;

    @Autowired
    FlaskService flaskService;

    @Autowired
    ReviewService reviewService;

    @Autowired
    APIService apiService;

    @GetMapping("/list")
    public String list(HttpSession session, Integer page, String keyword, Integer pageSize, String sortKey, String sortOrder, String cateKey, Model m) throws Exception {

        // 권한이 있는 id인지 확인 후 권한이 있으면 모델에 넘기기 (관리자 페이지 용도)
        String id = (String) session.getAttribute("id");
        validateAdmin(id, m);

        // 페이징 정보 초기값 설정
        if (page == null) page = 1;
        if (pageSize == null) pageSize = 12;
        if (sortKey == null) sortKey = "date";
        if (sortOrder == null) sortOrder = "desc";
        if (keyword == null) keyword = "";

        // 페이징에 정보 맵에 저장
        Map<String, Object> map = new HashMap<>();
        map.put("offset", (page - 1) * pageSize);
        map.put("pageSize", pageSize);
        map.put("sortKey", sortKey);
        map.put("sortOrder", sortOrder);
        map.put("cateKey", cateKey);
        map.put("keyword", keyword);

        SearchCondition sc = new SearchCondition(map);

        // 한 페이지 정보 가져오기
        List<ProductDto> prodList = productService.getPage(sc);

        // 카테고리화된 상품의 크기 반환 후 페이지 핸들러에 전달
//        int filteredTotalCnt = productService.getFilteredAndSortedTotalSize(map);
        int getPageSize = productService.listSize(sc);
        PageHandler pageHandler = new PageHandler(getPageSize, page, pageSize);


        // 모든 카테고리 정보 가져오기 (카테고리 선택 버튼)
        List<CategoryDto> cateList = categoryService.getAllCategories();

        // (카테고리화, 정렬, 페이징이 된) 한 페이지의 상품 리스트
        m.addAttribute("prodList", prodList);
        // 전체 카테고리 정보
        m.addAttribute("cateList", cateList);
        // 페이지 핸들러
        m.addAttribute("ph", pageHandler);
        // 카테고리 키
        m.addAttribute("cateKey", cateKey);
        // 정렬 키와 정렬 순서 모델에 추가
        m.addAttribute("sortKey", sortKey);
        m.addAttribute("sortOrder", sortOrder);
        // 키워드
        m.addAttribute("keyword", keyword);

        return "product/productList";
    }

    private void validateAdmin(String id, Model m) {
        if (true) { // 권한 확인하는 조건
            m.addAttribute("admin", "admin");
        }
    }

    @GetMapping("/detail")
    public String detail(HttpServletRequest request, String prodId, Model m) throws Exception {
        // 클라이언트로부터 받아온 상품id로 상품 dto 읽어와서 모델에 담기 (상세 페이지에 출력 용도)
        ProductDto pdto = productService.readProductDetail(prodId);
        m.addAttribute("pdto", pdto);

        // 클라이언트로 받아온 상품id로 저자 dto 읽어와서 모델에 담기 (상세 페이지에 출력 용도)
        AuthorDto adto = productService.getAuthorInfo(prodId);
        m.addAttribute("adto", adto);

        // 상품의 카테고리 코드로 카테고리 네임 반환 후 모델에 담기 (상세 페이지에 출력 용도)
        String cateCode = pdto.getCateCode();
        String cateName = productService.getCateName(cateCode);
        m.addAttribute("cateName", cateName);

        // 세션에서 custId 받아와서 모델에 추가
        HttpSession session = request.getSession();
        String custId = (String) session.getAttribute("custId");
        if (custId != null) {
            m.addAttribute("custId", custId);
        }

        int reviewCount = reviewService.reviewCountPerProduct(prodId);
        int positiveReviewCount = reviewService.positiveReviewCountPerProduct(prodId);
        int negativeReviewCount = reviewService.negativeReviewCountPerProduct(prodId);
        int pendingReviewCount = reviewService.pendingReviewCountPerProduct(prodId);

        m.addAttribute("totalReviewCount", reviewCount);
        m.addAttribute("totalPositiveReviewCount", positiveReviewCount);
        m.addAttribute("totalNegativeReviewCount", negativeReviewCount);
        m.addAttribute("totalPendingReviewCount", pendingReviewCount);


        // prodId를 flask 서버로 전송
        Map toFlask = new HashMap();
        toFlask.put("prodId", prodId);
        flaskService.sendDataToFlask(toFlask,"receive-prod-id");


        return "product/productDetail";
    }

    @PostMapping("/detail")
    public void detail(@RequestBody Map<String, Object> map) throws Exception {

        String prodId = map.get("prodId").toString();
        String custId = map.get("custId").toString();
        int itemQty = Integer.parseInt(map.get("itemQty").toString());

        // CustomerProduct dto가 있으면 그거 유지
        CustomerProductDto existingDto = productService.getCustomerProduct(custId, prodId);

        // CustomerProduct dto가 없으면 생성
        if (existingDto == null) {
            CustomerProductDto newDto = new CustomerProductDto(custId, prodId);
            productService.insertCustomerProduct(newDto);
        }


        for (int i = 0; i < itemQty; i++) {
            productService.plusSales(prodId);
        }

    }

//    @PostMapping("/detail/translate")
//    @ResponseBody
//    public ResponseEntity<String> translate(@RequestBody String text) throws Exception {
//        String translatedText = apiService.translateText(text);  // 번역 서비스 호출
//        return ResponseEntity.ok(translatedText);  // 번역 결과 반환
//    }


    @GetMapping("/manage")
    public String manage(Model m) throws Exception {
        // 모든 카테고리 정보 모델에 담기 (셀렉트 버튼에 띄우기 용)
        List<CategoryDto> cateList = categoryService.getAllFinalCategories();
        m.addAttribute("cateList", cateList);

        // 모든 저자 정보 모델에 담기 (셀렉트 버튼에 띄우기 용)
        List<AuthorDto> authList = authorService.getAllAuthorOrderedByName();
        m.addAttribute("authList", authList);

        return "product/manage";
    }

    private String uploadPath = System.getProperty("user.dir") + "/src/main/resources/static/images/product";

    @PostMapping("/manage/add")
    public String addProduct(
            @ModelAttribute ProductDto productDto,
            @RequestParam("prodImg") MultipartFile prodImg,
            RedirectAttributes redirectAttributes) throws Exception {

        // 파일 업로드 처리
        if (!prodImg.isEmpty()) {
            String fileName = prodImg.getOriginalFilename();
            System.out.println(fileName);

            // 저장할 경로 지정
            File directory = new File(uploadPath);
            if (!directory.exists()) {
                directory.mkdirs(); // 디렉토리 생성
            }

            File upFile = new File(uploadPath, fileName);
            prodImg.transferTo(upFile);

            // 업로드한 파일의 이름을 productDto의 imageId 필드에 설정
            productDto.setImageId(fileName);
        }

        // 상품 정보 저장
        productService.addProduct(productDto);

        // 성공 메시지를 추가
        redirectAttributes.addFlashAttribute("message", "상품이 성공적으로 등록되었습니다.");

        // product/list로 리다이렉트
        return "redirect:/product/list";
    }

    @PostMapping("/manage/view" )
    @ResponseBody
    public ProductDto viewProduct(@RequestParam("prodId") String prodId) throws Exception {
        // 클라이언트로부터 받아온 상품 id로 상품 조회
        return productService.select(prodId);
    }

    @PostMapping("/manage/update")
    public String updateProduct(
            @ModelAttribute ProductDto productDto,
            @RequestParam(value = "prodImg", required = false) MultipartFile prodImg) throws Exception {

        // 새로운 이미지 파일 저장 로직
        if (prodImg != null && !prodImg.isEmpty()) {
            String newFileName = prodImg.getOriginalFilename();

            // 새로운 이미지 파일 저장
            File upFile = new File(uploadPath, newFileName);
            prodImg.transferTo(upFile);

            // 새로운 이미지 파일명을 ProductDto에 지정
            productDto.setImageId(newFileName);
        }

        // 상품 정보 업데이트
        productService.updateProduct(productDto);

        return "redirect:/product/manage";
    }

    @PostMapping("/manage/delete")
    public String deleteProduct(@RequestParam("prodId") String prodId) throws Exception {
        // 클라이언트로부터 받아온 상품 id로 해당 상품 삭제
        productService.removeProduct(prodId);
        return "redirect:/product/manage";
    }

    @GetMapping("/manage/review")
    public String flaskTest(Model model) throws Exception {

        int totalReviewCount = reviewService.totalReviewCount();
        int totalPositiveReviewCount = reviewService.totalPositiveReviewCount();
        int totalNegativeReviewCount = reviewService.totalNegativeReviewCount();
        int totalPendingReviewCount = reviewService.totalPendingReviewCount();

        model.addAttribute("totalReviewCount", totalReviewCount);
        model.addAttribute("totalPositiveReviewCount", totalPositiveReviewCount);
        model.addAttribute("totalNegativeReviewCount", totalNegativeReviewCount);
        model.addAttribute("totalPendingReviewCount", totalPendingReviewCount);


        return "product/manageReview";
    }
    // 감정 분석 요청을 처리하는 엔드포인트
    @ResponseBody
    @PostMapping("/manage/review")
    public String analyzeReviews() {
        try {
            // Flask로 감정 분석 요청 보내기
            String sentimentResponse = flaskService.sendDataToFlask(new HashMap<>(), "review-sentiment");

            // Flask로부터 받은 응답을 그대로 반환 (JSON 형식)
            return sentimentResponse;
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"status\": \"error\", \"message\": \"" + e.getMessage() + "\"}";
        }
    }




    @ExceptionHandler(DataIntegrityViolationException.class)
    public String dataInteVioEx(Exception ex, Model m) {
        m.addAttribute("message", "데이터 무결성을 위반했습니다. 확인하고 다시 요청해주세요.");
        return "product/error";
    }
    @ExceptionHandler(SQLException.class)
    public String sqlEx(Exception ex, Model m) {
        m.addAttribute("message", "쿼리 실행 중 예외가 발생했습니다.");
        return "product/error";
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public String mthArgNotValiEx(Exception ex, Model m) {
        m.addAttribute("message", "올바르지 않은 값을 입력했습니다. 확인하고 다시 요청해주세요.");
        return "product/error";
    }
    @ExceptionHandler(Exception.class)
    public String handleAllExceptions(Exception ex, Model model) {
        System.err.println("예외 발생: " + ex.getClass().getName());
        ex.printStackTrace();

        model.addAttribute("message", "알 수 없는 에러가 발생했습니다.");

        return "product/error";
    }

}
