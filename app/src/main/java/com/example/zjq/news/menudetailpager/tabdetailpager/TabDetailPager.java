package com.example.zjq.news.menudetailpager.tabdetailpager;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.bumptech.glide.Glide;
import com.example.myrefreshview.RefreshListView;
import com.example.zjq.news.R;
import com.example.zjq.news.activity.NewsDetailActivity;
import com.example.zjq.news.base.MenuDetailBasePager;
import com.example.zjq.news.bean.NewsCenterPagerBean;
import com.example.zjq.news.menudetailpager.adapter.MyListviewAdapter;
import com.example.zjq.news.menudetailpager.bean.TabDetailBean;
import com.example.zjq.news.menudetailpager.bean.TabDetailViewPagerBean;
import com.example.zjq.news.utils.CacheUtils;
import com.example.zjq.news.utils.Constants;
import com.example.zjq.news.view.HorizontalScrollViewPager;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONObject;
import org.xutils.common.Callback;
import org.xutils.common.util.DensityUtil;
import org.xutils.http.RequestParams;
import org.xutils.x;

import java.util.ArrayList;
import java.util.List;

public class TabDetailPager extends MenuDetailBasePager {

    public static final String READ_ARRAY_ID = "read_array_id";
    private NewsCenterPagerBean.DataBean data;

    private List<String> imgs;

    private HorizontalScrollViewPager viewPager;

    private TextView tv_title;

    private LinearLayout ll_point_group;

    private RefreshListView listView;

    //之前高亮点位置
    private int prePosition;

    private MyListviewAdapter adapter;
    private List<TabDetailBean> list;
    private List<TabDetailBean> list_more;
    private int start = 0;
    private int end = 5;
    private MyHandler myhandler;
    private TabDetailViewPagerBean bean;


    public TabDetailPager(Context context, NewsCenterPagerBean.DataBean dataBean) {
        super(context);
        this.data = dataBean;

    }

    @Override
    public View initView() {

        View view = View.inflate(context, R.layout.tabdetail_pager, null);

        View topNewxView = View.inflate(context, R.layout.topnewx, null);
        viewPager = topNewxView.findViewById(R.id.viewpager);
        tv_title = topNewxView.findViewById(R.id.tv_title);
        ll_point_group = topNewxView.findViewById(R.id.ll_point_group);
        listView = view.findViewById(R.id.listview);

//        listView.addHeaderView(topNewxView);
        listView.addTopNewsView(topNewxView);

        //设置监听刷新
        listView.setOnRefreshListener(new MyOnRefreshListener());

        //设置listViewitem的点击监听
        listView.setOnItemClickListener(new OnItemClickListener());

        return view;
    }

    class OnItemClickListener implements AdapterView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

            int realPostion = i - 1;
            TabDetailBean data = list.get(realPostion);

//            Toast.makeText(context, data.getMtime(), Toast.LENGTH_SHORT).show();

            //取出保存的id数组
            String idArray = CacheUtils.getString(context, READ_ARRAY_ID);
            //判断是否存在，不存在 则保存，变灰
            if (!idArray.contains(data.getDocid())) {
                //
                CacheUtils.setString(context, READ_ARRAY_ID, idArray + data.getDocid() + ",");

                //刷新
                adapter.notifyDataSetChanged();//getCount--getView
            }

            //跳转到新闻详情页面
            Intent intent = new Intent(context, NewsDetailActivity.class);
            intent.putExtra("url", data.getUrl());
            intent.putExtra("source", data.getSource());
            context.startActivity(intent);
        }
    }

    class MyOnRefreshListener implements RefreshListView.OnRefreshListener {

        @Override
        public void onPullDownRefresh() {

            getDataForImgs();
            getDataForRecyclerview(1);
        }

        @Override
        public void onLoadMore() {

            getDataForRecyclerview(1);
        }
    }

    @Override
    public void initData() {
        super.initData();

//        //下方list view
//        HtmlAsynTask task = new HtmlAsynTask();
//        task.execute(url);


        getDataForImgs();


        adapter = new MyListviewAdapter(context);
        listView.setAdapter(adapter);
        //下方数据


            getDataForRecyclerview(2);



    }

    private void getDataForImgs() {
        String newsId = data.getNewsId();
        String url = Constants.NewsDetail;

        RequestParams params = new RequestParams(url);
        params.addBodyParameter("app_id", Constants.APPID);
        params.addBodyParameter("app_secret", Constants.APPSECRET);
        params.addBodyParameter("newsId", newsId);

        x.http().get(params, new Callback.CommonCallback<String>() {
            @Override
            public void onSuccess(String result) {

                bean = new Gson().fromJson(result, TabDetailViewPagerBean.class);

                imgs = new ArrayList<>();

                //添加外层图片
                if (data.getImgList() != null && data.getImgList().size() != 0) {
                    for (int i = 0; i < data.getImgList().size(); i++) {
                        if (!TextUtils.isEmpty(data.getImgList().get(i))) {
                            imgs.add(data.getImgList().get(i));
                        }
                    }
                }

                //加载上方轮播图

                List<TabDetailViewPagerBean.DataBean.ImgDataBean> list_imgs = bean.getData().getImages();
                int size = list_imgs.size();

                if (size > 4) {
                    size = 4;
                }
                //添加内层图片
                for (int i = 0; i < size; i++) {
                    if (list_imgs.get(i).getImgSrc() != null && !TextUtils.isEmpty(list_imgs.get(i).getImgSrc())) {
                        imgs.add(list_imgs.get(i).getImgSrc());
                    }
                }


                //设置viewpager的适配器
                viewPager.setAdapter(new TabDetailPagerTopNewsAdapter());

                //红点
                RedPoint();

                //轮播图（Handler实现）
                if (myhandler == null) {
                    myhandler = new MyHandler();
                }

                myhandler.removeCallbacksAndMessages(null);
                //三秒之后，执行Runnable中的run方法（new的时候再子线程，run方法就执行在主线程 ）
//                myhandler.postDelayed(new MyRunnable(), 3000);
                myhandler.sendEmptyMessageDelayed(0, 3000);
            }

            @Override
            public void onError(Throwable ex, boolean isOnCallback) {

                Log.e("zjq-result-aa", ex.getMessage() + "");
            }

            @Override
            public void onCancelled(CancelledException cex) {

            }

            @Override
            public void onFinished() {

            }
        });

    }

//    class MyRunnable implements Runnable {
//
//        @Override
//        public void run() {
//
//            myhandler.sendEmptyMessage(0);
//        }
//    }

    class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            try {
                //切换viewaPager下一个页面
                int item = (viewPager.getCurrentItem() + 1) % imgs.size();
                viewPager.setCurrentItem(item);

                myhandler.removeCallbacksAndMessages(null);
                myhandler.sendEmptyMessageDelayed(0, 3000);
            } catch (Exception e) {

                Log.e("zjq-red", e.getMessage());
            }


        }
    }


    private void getDataForRecyclerview(int from) {

        final String url = "https://api.xiaohuwei.cn/news.php";

        RequestParams params = new RequestParams(url);
        //PS：type 参数可为 hot（精选）yule (娱乐) toutiao (头条)motion (运动) 不传默认为 hot
        params.addBodyParameter("type", "toutiao");


        if (from==2){

            getdata(params);

        }else {

           if (TextUtils.isEmpty(CacheUtils.getString(context,"listView_news"))){
               getdata(params);
           }else {
               setListViewData(CacheUtils.getString(context,"listView_news"));

           }


        }





    }

    private void getdata(RequestParams params) {
        x.http().get(params, new Callback.CommonCallback<String>() {
            @Override
            public void onSuccess(String result) {

                setListViewData(result);

                //保存数据
                CacheUtils.setString(context,"listView_news",result);

            }

            @Override
            public void onError(Throwable ex, boolean isOnCallback) {

                Log.e("zjq-aa", ex.getMessage());

                //隐藏下拉刷新控件
                listView.onRefreshFinish(true);

                //没有更多数据
                listView.NoMore();

            }

            @Override
            public void onCancelled(CancelledException cex) {

            }

            @Override
            public void onFinished() {

            }
        });
    }

    private void setListViewData(String result) {
        Log.e("zjq-rrr",result);

        try {

            JSONArray jsonArray = new JSONArray(result);
            list = new ArrayList<>();

            for (int i = 1; i < jsonArray.length(); i++) {

                JSONObject jsonObject = (JSONObject) jsonArray.get(i);

                if (jsonObject != null) {
                    TabDetailBean bean = new TabDetailBean(
                            jsonObject.optString("mtime"),
                            jsonObject.optString("digest"),
                            jsonObject.optString("imgsrc"),
                            jsonObject.optString("url"),
                            jsonObject.optString("docid"),
                            jsonObject.optString("source"));

                    list.add(bean);
                }


            }

            //下拉刷新
//                    list_more = list.subList(start, end);

            adapter.setData(list);

            //隐藏下拉刷新控件-更新时间(true)
            listView.onRefreshFinish(true);





        } catch (Exception e) {

            Log.e("zjq.999",e.getMessage());

            //隐藏下拉刷新控件-更新时间(true)
            listView.onRefreshFinish(true);

            //没有更多数据
            listView.NoMore();
        }
    }


    private void RedPoint() {
        ll_point_group.removeAllViews();

        for (int i = 0; i < imgs.size(); i++) {
            ImageView imageView = new ImageView(context);
            imageView.setBackgroundResource(R.drawable.point_selector);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(DensityUtil.dip2px(8), DensityUtil.dip2px(8));


            if (i == 0) {
                imageView.setEnabled(true);
            } else {
                imageView.setEnabled(false);

                params.leftMargin = DensityUtil.dip2px(8);
            }

            imageView.setLayoutParams(params);
            ll_point_group.addView(imageView);
        }

        //监听页面得额改变
        viewPager.addOnPageChangeListener(new MyOnPageChangeListener());
//        tv_title.setText(data.getTitle());

    }


    class MyOnPageChangeListener implements ViewPager.OnPageChangeListener {

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {

            //设置文本
            tv_title.setText(data.getTitle());

            //把之前的变成灰色，
            ll_point_group.getChildAt(prePosition).setEnabled(false);

            ll_point_group.getChildAt(position).setEnabled(true);

            prePosition = position;

        }

        private boolean isDradding = false;

        @Override
        public void onPageScrollStateChanged(int state) {

            if (state == ViewPager.SCROLL_STATE_DRAGGING) {//拖拽

                isDradding = true;
                //移除\
                myhandler.removeCallbacksAndMessages(null);
            } else if (state == ViewPager.SCROLL_STATE_SETTLING && isDradding) {//惯性
                myhandler.removeCallbacksAndMessages(null);

                myhandler.sendEmptyMessageDelayed(0, 3000);
                isDradding = false;
            } else if (state == ViewPager.SCROLL_STATE_IDLE && isDradding) {//静止
                isDradding = false;
                myhandler.removeCallbacksAndMessages(null);

                myhandler.sendEmptyMessageDelayed(0, 3000);
            }


        }
    }

    class TabDetailPagerTopNewsAdapter extends PagerAdapter {

        @Override
        public int getCount() {
            return imgs.size();
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object o) {
            return view == o;
        }

        @SuppressLint("ClickableViewAccessibility")
        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {

            ImageView imageView = new ImageView(context);
            imageView.setScaleType(ImageView.ScaleType.FIT_XY);

            imageView.setBackgroundResource(R.drawable.home_scroll_default);
            container.addView(imageView);

            Glide.with(context).load(imgs.get(position)).placeholder(R.drawable.news_pic_default).into(imageView);

            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //跳转到新闻详情页面
                    Intent intent = new Intent(context, NewsDetailActivity.class);
                    intent.putExtra("url", bean.getData().getContent());
                    intent.putExtra("source", data.getSource());
                    intent.putExtra("from", "top");
                    context.startActivity(intent);
                }
            });

//            imageView.setOnTouchListener(new View.OnTouchListener() {
//                @Override
//                public boolean onTouch(View view, MotionEvent motionEvent) {
//                    switch (motionEvent.getAction()) {
//                        case MotionEvent.ACTION_DOWN://按下
//                            myhandler.removeCallbacksAndMessages(null);
//
//                            break;
//                        case MotionEvent.ACTION_UP://离开
//                            myhandler.removeCallbacksAndMessages(null);
//
//                            myhandler.sendEmptyMessageDelayed(0, 1000);
//
//                            break;
//
//
//                    }
//
//                    //只有触摸事件可以返回true,即不往下传递了
//                    return true;
//                }
//            });

            return imageView;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {

            container.removeView((View) object);
        }
    }

/*    class HtmlAsynTask extends AsyncTask<String, Integer, Document> {


        @Override
        protected Document doInBackground(String... strings) {

            try {
                Document doc = Jsoup.connect(strings[0]).get();

                return doc;
            } catch (IOException e) {

                Log.e("zjq-e", e.getMessage());

                return null;
            }


        }

        @Override
        protected void onPostExecute(Document doc) {
            super.onPostExecute(doc);

            if (doc != null) {

                String title = doc.title();

            }

        }
    }*/


}
