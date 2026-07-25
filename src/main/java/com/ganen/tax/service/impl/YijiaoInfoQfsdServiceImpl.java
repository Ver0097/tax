package com.ganen.tax.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ganen.tax.entity.YijiaoInfoQfsd;
import com.ganen.tax.mapper.YijiaoInfoQfsdMapper;
import com.ganen.tax.service.YijiaoInfoQfsdService;
import org.springframework.stereotype.Service;

/**
 * 已缴税费信息（区分税地） Service 实现
 */
@Service
public class YijiaoInfoQfsdServiceImpl extends ServiceImpl<YijiaoInfoQfsdMapper, YijiaoInfoQfsd> implements YijiaoInfoQfsdService {
}
