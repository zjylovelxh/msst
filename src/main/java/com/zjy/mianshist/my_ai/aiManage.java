package com.zjy.mianshist.my_ai;



import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionChoice;
import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionRequest;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessage;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessageRole;

import com.volcengine.ark.runtime.service.ArkService;
import com.zjy.mianshist.common.ErrorCode;
import com.zjy.mianshist.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
public class aiManage {


   @Resource
   private ArkService aiservice;

   public  String aichat(String systemprompt, String userprompt){
       final List<ChatMessage> messages = new ArrayList<>();

       //系统预设
       final ChatMessage systemMessage = ChatMessage.builder().role(ChatMessageRole.SYSTEM).content(systemprompt).build();
       //用户预设
       final ChatMessage userMessage = ChatMessage.builder().role(ChatMessageRole.USER).content(userprompt).build();

       messages.add(systemMessage);
       messages.add(userMessage);
       ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
               .model("deepseek-v3-250324")
               .messages(messages)
               .build();
       List<ChatCompletionChoice> choices = aiservice.createChatCompletion(chatCompletionRequest).getChoices();
       if (choices != null){
          return (String)choices.get(0).getMessage().getContent();
       }else {
           throw new BusinessException(ErrorCode.OPERATION_ERROR,"Ai 调用失败！");
       }


       //service.shutdownExecutor();
   }

}


