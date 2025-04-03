export const lineOptions = {
    title: {
        text: '折线图示例',
        left: 'center',
    },
    tooltip: {
        trigger: 'axis',
    },
    xAxis: {
        type: 'category',
        data: ['一月', '二月', '三月', '四月', '五月', '六月'],
    },
    yAxis: {
        type: 'value',
    },
    series: [
        {
            name: '销量',
            type: 'line',
            data: [150, 230, 224, 218, 135, 147],
            smooth: true, // 平滑曲线
        },
    ],
};