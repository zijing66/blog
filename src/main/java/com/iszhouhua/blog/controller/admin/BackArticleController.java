package com.iszhouhua.blog.controller.admin;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.iszhouhua.blog.common.util.Result;
import com.iszhouhua.blog.common.util.ValidatorUtils;
import com.iszhouhua.blog.model.Article;
import com.iszhouhua.blog.model.Tag;
import com.iszhouhua.blog.model.enums.ArticleStatusEnum;
import com.iszhouhua.blog.service.ArticleService;
import com.iszhouhua.blog.service.ArticleTagService;
import com.iszhouhua.blog.service.TagService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 后台文章控制器
 * @author ZhouHua
 * @since 2018-12-20
 */
@RestController
@RequestMapping("admin/article")
public class BackArticleController {

    @Autowired
    private ArticleService articleService;
    @Autowired
    private TagService tagService;
    @Autowired
    private ArticleTagService articleTagService;

    @GetMapping
    public Result list(Page<Article> page, Article article){
        //title需使用模糊查询，单独处理
        String title=article.getTitle();
        article.setTitle(null);
        QueryWrapper<Article> queryWrapper=new QueryWrapper<>(article);
        if(article.getStatus()==null){
            queryWrapper.in("status",ArticleStatusEnum.DRAFT.getValue(),ArticleStatusEnum.PUBLISHED.getValue());
        }
        if(StringUtils.isNotBlank(title)){
            queryWrapper.like(true,"title",title);
        }
        IPage<Article> articlePage=articleService.page(page,queryWrapper);
        articlePage.getRecords().forEach(post -> post.setTags(tagService.findTagsByArticleId(post.getId())));
        return Result.success("查询成功",articlePage);
    }

    @PostMapping
    public Result save(@RequestBody Article article){
        ValidatorUtils.validate(article);
        if(StringUtils.isBlank(article.getUrl())){
            article.setUrl(article.getTitle());
        }
        article.setUpdateTime(new Date());
        boolean res=articleService.saveOrUpdate(article);
        if(!res){
            return Result.fail("保存失败");
        }
        if(!CollectionUtils.isEmpty(article.getTags())){
            Long articleId=article.getId()!=null?article.getId():articleService.findArticleByUrl(article.getUrl()).getId();
            List<Long> tagIds=article.getTags().stream().map(Tag::getId).collect(Collectors.toList());
            res=articleTagService.saveBatch(articleId,tagIds);
            if(!res){
                return Result.fail("文章已成功保存，但关联标签保存失败");
            }
        }
        if(article.getId()==null){
            //将新增的文章查询出来
            article=articleService.getOne(new QueryWrapper<>(article));
        }
        return Result.success("保存成功",article);
    }

    @PutMapping
    public Result info(Long id){
        Article article=articleService.getById(id);
        article.setTags(tagService.findTagsByArticleId(article.getId()));
        return Result.success("查询成功",article);
    }

    @DeleteMapping
    public Result remove(Long id){
        return articleService.removeById(id)?Result.success("删除成功"):Result.fail("删除失败");
    }
}
