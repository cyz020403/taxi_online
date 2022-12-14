package com.neo.web.DriverViewsController;

import com.neo.mapper.*;
import com.neo.model.Car;
import com.neo.model.Driver;
import com.neo.model.Passenger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLOutput;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

@RestController
public class StartWorkController {

    @Autowired
    WorkMapper workMapper;

    @Autowired
    CarMapper carMapper;

    @Autowired
    DriverMapper driverMapper;

    @Autowired
    PassengerMapper passengerMapper;

    @Autowired
    TaxiOrderMapper taxiOrderMapper;

    @RequestMapping("/selectCarAndCreateWorkRecord")
    public Car SelectCarAndCreateWorkRecord(@RequestBody Map<String, Object> myData) {
        String selectedCarPlate = myData.get("plate").toString();
        Car nowCar = carMapper.getCarByPlate(selectedCarPlate);

        String nowDriverUserName = myData.get("userName").toString();
        Driver nowDriver = driverMapper.getOneByUsername(nowDriverUserName);

        Date nowDate = new Date();
        SimpleDateFormat ft = new SimpleDateFormat ("yyyy-MM-dd HH:mm:ss");
        ft.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));

        workMapper.insertNewWorkingUntransing(nowDriver.getId(),nowCar.getId(),ft.format(nowDate));

        return nowCar;
    }

    @RequestMapping("/stopWorkByDriverUserNameAndPlate")
    public void StopWorkByDriverUserNameAndPlate(@RequestBody Map<String, Object> myData) {
//        System.out.println(myData);
        String selectedCarPlate = myData.get("plate").toString();
        Car nowCar = carMapper.getCarByPlate(selectedCarPlate);

        String nowDriverUserName = myData.get("userName").toString();
        Driver nowDriver = driverMapper.getOneByUsername(nowDriverUserName);


        Date nowDate = new Date();
        SimpleDateFormat ft = new SimpleDateFormat ("yyyy-MM-dd HH:mm:ss");
        TimeZone time = TimeZone.getTimeZone("Etc/GMT-8");  //?????????????????????
        TimeZone.setDefault(time);
        workMapper.updateEndtimeByDriverAndCarId(nowDriver.getId(), nowCar.getId(), ft.format(nowDate));

        workMapper.updateToUnworkingByDriverAndCarId(nowDriver.getId(), nowCar.getId());
    }

    // ???????????????????????????
    // ????????? carId driverId isTransing=true
    @RequestMapping("/queryMyOrder")
    public Map<String, Object> queryMyOrder(@RequestBody Map<String, Object> myData) {
//        System.out.println(myData);

        String driverUserName = myData.get("driverUserName").toString();
        String carPlate = myData.get("carPlate").toString();
        List<Map<String, Object>> returnData =  workMapper.searchMyOrder(driverUserName, carPlate);
        if(returnData.size() == 0) {
            return null;
        }

//        System.out.println(returnData.get(0));

        //???????????????update??????work??????isTransing??? ??????????????????????????????????????????

        return returnData.get(0);
    }

    @RequestMapping("/modifyCarPosition")
    public void modifyCarPosition(@RequestBody Map<String, Object> myData) {
        // map ??????: lng, lat, carPlate, isTransing, passengerUserName
        System.out.println(myData);
        String lng = myData.get("lng").toString();
        String lat = myData.get("lat").toString();
        String carPlate = myData.get("carPlate").toString();
        carMapper.modifyCarPosition(lng, lat, carPlate);

        String isTransing = myData.get("isTransing").toString();
        String passengerUserName = myData.get("passengerUserName").toString();
        if(isTransing.equals("true")) {
            passengerMapper.modifyPosition(lng, lat, passengerUserName);
        }
    }

    @RequestMapping("/searchCarPosition")
    public Map<String, Object> searchCarPosition(@RequestBody Map<String, Object> myData) {
        // map ??????: carPlate
        // return ??????: lng, lat
        String carPlate = myData.get("carPlate").toString();
        return carMapper.searchCarPosition(carPlate);
    }

    //???????????????????????????work???order ????????????
    @RequestMapping("/finishOrder")
    public void finishOrder(@RequestBody Map<String, Object> myData) {
        //Map: driverUserName, passengerUserName, plate, price, isReserve

        String driverUserName = myData.get("driverUserName").toString();
        String passengerUserName = myData.get("passengerUserName").toString();
        String plate = myData.get("plate").toString();
        String price = myData.get("price").toString();
        String isReserve = myData.get("isReserve").toString();

        Driver nowDriver = driverMapper.getOneByUsername(driverUserName);
        Passenger nowPassenger = passengerMapper.getOneByUsername(passengerUserName);
        Car nowCar = carMapper.getCarByPlate(plate);

//        driverMapper.updateBalance(String driverUserName, String price);
        passengerMapper.updateBalance(passengerUserName, price);

        workMapper.updateWorkingWorkToUnTransing(nowDriver.getId(), nowCar.getId());

        Date nowDate = new Date();
        SimpleDateFormat ft = new SimpleDateFormat ("yyyy-MM-dd HH:mm:ss");
        ft.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
        if(isReserve.equals("false")) {
            taxiOrderMapper.updateIsTransingAndEndTime
                    (nowDriver.getId(), nowCar.getId(), nowPassenger.getId(), ft.format(nowDate));
        }
        else {
            taxiOrderMapper.updateIsTransingAndEndTimeReserve
                    (nowDriver.getId(), nowCar.getId(), nowPassenger.getId(), ft.format(nowDate));
        }
        //??????driver ?????????
        // ??????passenger?????????
        // ??????work???isTransing
        // ?????? taxi_order ???isTrasning ??????endTime

        // ??????????????????????????????????????????????????????????????????
    }

    @RequestMapping("queryAllowedWork")
    public Map<String, Object> queryAllowedWork(@RequestBody Map<String, Object> myData) {
        return driverMapper.getAllowedCondition(myData.get("driverUserName").toString());
    }
}
