package com.ganen.tax.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ganen.tax.entity.YukouInfo;
import com.ganen.tax.mapper.YukouInfoMapper;
import com.ganen.tax.service.YukouInfoService;
import org.springframework.stereotype.Service;

@Service
public class YukouInfoServiceImpl extends ServiceImpl<YukouInfoMapper, YukouInfo> implements YukouInfoService {
}