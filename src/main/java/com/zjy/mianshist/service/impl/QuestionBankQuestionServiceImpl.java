package com.zjy.mianshist.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zjy.mianshist.common.ErrorCode;
import com.zjy.mianshist.constant.CommonConstant;
import com.zjy.mianshist.exception.BusinessException;
import com.zjy.mianshist.exception.ThrowUtils;
import com.zjy.mianshist.mapper.QuestionBankQuestionMapper;
import com.zjy.mianshist.model.dto.questionBankQuestion.QuestionBankQuestionQueryRequest;
import com.zjy.mianshist.model.entity.Question;
import com.zjy.mianshist.model.entity.QuestionBank;
import com.zjy.mianshist.model.entity.QuestionBankQuestion;
import com.zjy.mianshist.model.entity.User;
import com.zjy.mianshist.model.vo.QuestionBankQuestionVO;
import com.zjy.mianshist.model.vo.UserVO;
import com.zjy.mianshist.service.QuestionBankQuestionService;
import com.zjy.mianshist.service.QuestionBankService;
import com.zjy.mianshist.service.QuestionService;
import com.zjy.mianshist.service.UserService;
import com.zjy.mianshist.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 题目题库关联表服务实现
 *

 */
@Service
@Slf4j
public class QuestionBankQuestionServiceImpl extends ServiceImpl<QuestionBankQuestionMapper, QuestionBankQuestion> implements QuestionBankQuestionService {

    @Resource

    private UserService userService;

    @Resource
    @Lazy
    private QuestionService questionService;

    @Resource
    @Lazy
    private QuestionBankService questionBankService;


    @Resource
    private RedissonClient redissonClient;
    /**
     * 校验数据
     *
     * @param questionBankQuestion
     * @param add      对创建的数据进行校验
     */
    @Override
    public void validQuestionBankQuestion(QuestionBankQuestion questionBankQuestion, boolean add) {
        ThrowUtils.throwIf(questionBankQuestion == null, ErrorCode.PARAMS_ERROR);


          ThrowUtils.throwIf(questionBankQuestion.getQuestionBankId()==null || questionBankQuestion.getQuestionId()==null,ErrorCode.NOT_FOUND_ERROR);
            if(questionBankQuestion.getQuestionBankId()!=null){
                ThrowUtils.throwIf(questionBankQuestion.getQuestionBankId()<=0, ErrorCode.PARAMS_ERROR);
                QuestionBank bankQuestion = questionBankService.getById(questionBankQuestion.getQuestionBankId());
                ThrowUtils.throwIf(bankQuestion==null,ErrorCode.NOT_FOUND_ERROR,"题库不存在！");
            }
            if(questionBankQuestion.getQuestionId()!=null){
                ThrowUtils.throwIf(questionBankQuestion.getQuestionId()<=0, ErrorCode.PARAMS_ERROR);
                Question question = questionService.getById(questionBankQuestion.getQuestionId());
                ThrowUtils.throwIf(question==null,ErrorCode.NOT_FOUND_ERROR,"题目不存在！");
            }


    }

    /**
     * 获取查询条件
     *
     * @param questionBankQuestionQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<QuestionBankQuestion> getQueryWrapper(QuestionBankQuestionQueryRequest questionBankQuestionQueryRequest) {
        QueryWrapper<QuestionBankQuestion> queryWrapper = new QueryWrapper<>();
        if (questionBankQuestionQueryRequest == null) {
            return queryWrapper;
        }
        // todo 从对象中取值
        Long id = questionBankQuestionQueryRequest.getId();
        Long notId = questionBankQuestionQueryRequest.getNotId();
        String sortField = questionBankQuestionQueryRequest.getSortField();
        String sortOrder = questionBankQuestionQueryRequest.getSortOrder();
        Long userId = questionBankQuestionQueryRequest.getUserId();
        Long questionBankId = questionBankQuestionQueryRequest.getQuestionBankId();
        Long questionId = questionBankQuestionQueryRequest.getQuestionId();


        // todo 补充需要的查询条件

        // 精确查询
        queryWrapper.ne(ObjectUtils.isNotEmpty(notId), "id", notId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(questionBankId),"questionBankId",questionBankId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(questionId),"questionId",questionId);
        // 排序规则
        queryWrapper.orderBy(SqlUtils.validSortField(sortField),
                sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    /**
     * 获取题目题库关联表封装
     *
     * @param questionBankQuestion
     * @param request
     * @return
     */
    @Override
    public QuestionBankQuestionVO getQuestionBankQuestionVO(QuestionBankQuestion questionBankQuestion, HttpServletRequest request) {
        // 对象转封装类
        QuestionBankQuestionVO questionBankQuestionVO = QuestionBankQuestionVO.objToVo(questionBankQuestion);

        // todo 可以根据需要为封装对象补充值，不需要的内容可以删除
        // region 可选
        // 1. 关联查询用户信息
        Long userId = questionBankQuestion.getUserId();
        User user = null;
        if (userId != null && userId > 0) {
            user = userService.getById(userId);
        }
        UserVO userVO = userService.getUserVO(user);
        questionBankQuestionVO.setUser(userVO);

        return questionBankQuestionVO;
    }

    /**
     * 分页获取题目题库关联表封装
     *
     * @param questionBankQuestionPage
     * @param request
     * @return
     */
    @Override
    public Page<QuestionBankQuestionVO> getQuestionBankQuestionVOPage(Page<QuestionBankQuestion> questionBankQuestionPage, HttpServletRequest request) {
        List<QuestionBankQuestion> questionBankQuestionList = questionBankQuestionPage.getRecords();
        Page<QuestionBankQuestionVO> questionBankQuestionVOPage = new Page<>(questionBankQuestionPage.getCurrent(), questionBankQuestionPage.getSize(), questionBankQuestionPage.getTotal());
        if (CollUtil.isEmpty(questionBankQuestionList)) {
            return questionBankQuestionVOPage;
        }
        // 对象列表 => 封装对象列表
        List<QuestionBankQuestionVO> questionBankQuestionVOList = questionBankQuestionList.stream().map(questionBankQuestion -> {
            return QuestionBankQuestionVO.objToVo(questionBankQuestion);
        }).collect(Collectors.toList());

        // todo 可以根据需要为封装对象补充值，不需要的内容可以删除
        // region 可选
        // 1. 关联查询用户信息
        Set<Long> userIdSet = questionBankQuestionList.stream().map(QuestionBankQuestion::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));

        questionBankQuestionVOList.forEach(questionBankQuestionVO -> {
            Long userId = questionBankQuestionVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            questionBankQuestionVO.setUser(userService.getUserVO(user));

        });
        // endregion

        questionBankQuestionVOPage.setRecords(questionBankQuestionVOList);
        return questionBankQuestionVOPage;
    }

    /**
     * 批量添加题目到题库
     * @param questionIdList
     * @param questionBankId
     * @param loginUser
     */
    @Override
    public void batchAddQuestionsToBank(List<Long> questionIdList, Long questionBankId, User loginUser) {

        RLock lock = redissonClient.getLock(loginUser.getUserRole());
        try {
            boolean isLocked = lock.tryLock(10, 30, TimeUnit.SECONDS);
            if (!isLocked) {
                throw new RuntimeException("获取锁失败，请稍后再试");
            }

        // 参数校验
        ThrowUtils.throwIf(CollUtil.isEmpty(questionIdList), ErrorCode.PARAMS_ERROR, "题目列表为空");
        ThrowUtils.throwIf(questionBankId == null || questionBankId <= 0, ErrorCode.PARAMS_ERROR, "题库非法");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);


        // 检查在题目表里存在的题目
        LambdaQueryWrapper<Question> questionLambdaQueryWrapper=new LambdaQueryWrapper<>();
        questionLambdaQueryWrapper.select(Question::getId).in(Question::getId,questionIdList);
        List<Long> validQuestionIdList = questionService.listObjs(questionLambdaQueryWrapper,obj -> (Long) obj);
        ThrowUtils.throwIf(CollUtil.isEmpty(validQuestionIdList), ErrorCode.PARAMS_ERROR, "合法的题目列表为空");


        // 检查题库 id 是否存在
        QuestionBank questionBank = questionBankService.getById(questionBankId);
        ThrowUtils.throwIf(questionBank == null, ErrorCode.NOT_FOUND_ERROR, "题库不存在");


        //已经在题库中的题目id
        LambdaQueryWrapper<QuestionBankQuestion> lambdaQueryWrapper=new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(QuestionBankQuestion::getQuestionBankId,questionBankId).in(QuestionBankQuestion::getQuestionId,validQuestionIdList).select(QuestionBankQuestion::getQuestionId);
        List<Long> inBankId = this.listObjs(lambdaQueryWrapper,obj -> (Long) obj);
        validQuestionIdList=validQuestionIdList.stream().filter(questionId ->{ return !inBankId.contains(questionId);}).collect(Collectors.toList());
       ThrowUtils.throwIf(validQuestionIdList.isEmpty(),ErrorCode.PARAMS_ERROR,"均已在题库中");


       //线程池
        ThreadPoolExecutor threadPoolExecutor=new ThreadPoolExecutor(30,50,1000, TimeUnit.MILLISECONDS,new ArrayBlockingQueue<>(1000),new ThreadPoolExecutor.DiscardPolicy());

        //所有批次任务集
        List<CompletableFuture<Void>> futures = new ArrayList<>();


        // 执行插入

        int rollsize=1000;
        int toatalsize=validQuestionIdList.size();
        for(int i=0;i<toatalsize;i+=rollsize){
            List<Long> idrolllist = validQuestionIdList.subList(i, Math.min(i + rollsize, toatalsize));
            List<QuestionBankQuestion> rolllist=idrolllist.stream().map(questionId -> {
                QuestionBankQuestion questionBankQuestion=new QuestionBankQuestion();
                questionBankQuestion.setQuestionBankId(questionBankId);
                questionBankQuestion.setQuestionId(questionId);
                questionBankQuestion.setUserId(loginUser.getId());
                return questionBankQuestion;
            }).collect(Collectors.toList());

            //获取当前类的代理对象
            QuestionBankQuestionService thisaop = (QuestionBankQuestionServiceImpl) AopContext.currentProxy();

            futures.add(CompletableFuture.runAsync(() -> {
                thisaop.batchAddQuestionsToBankInner(rolllist);
            }, threadPoolExecutor));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        threadPoolExecutor.shutdown();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("线程被中断", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchAddQuestionsToBankInner(List<QuestionBankQuestion> questionBankQuestions) {
            try {
                boolean result = this.saveBatch(questionBankQuestions);
                ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "向题库添加题目失败");
            } catch (DataIntegrityViolationException e) {
                log.error("数据库唯一键冲突或违反其他完整性约束, 错误信息: {}", e.getMessage());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "题目已存在于该题库，无法重复添加");
            } catch (DataAccessException e) {
                log.error("数据库连接问题、事务问题等导致操作失败, 错误信息: {}", e.getMessage());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "数据库操作失败");
            } catch (Exception e) {
                // 捕获其他异常，做通用处理
                log.error("添加题目到题库时发生未知错误，错误信息: {}", e.getMessage());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "向题库添加题目失败");
            }
    }


    /**
     * 批量从题库删除题目
     * @param questionIdList
     * @param questionBankId
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchRemoveQuestionsFromBank(List<Long> questionIdList, Long questionBankId) {
        // 参数校验
        ThrowUtils.throwIf(CollUtil.isEmpty(questionIdList), ErrorCode.PARAMS_ERROR, "题目列表为空");
        ThrowUtils.throwIf(questionBankId == null || questionBankId <= 0, ErrorCode.PARAMS_ERROR, "题库非法");
        // 执行删除关联
        for (Long questionId : questionIdList) {
            // 构造查询
            LambdaQueryWrapper<QuestionBankQuestion> lambdaQueryWrapper = Wrappers.lambdaQuery(QuestionBankQuestion.class)
                    .eq(QuestionBankQuestion::getQuestionId, questionId)
                    .eq(QuestionBankQuestion::getQuestionBankId, questionBankId);
            boolean result = this.remove(lambdaQueryWrapper);
            if (!result) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "从题库移除题目失败");
            }
        }
    }



}
