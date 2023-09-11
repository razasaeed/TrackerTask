package com.tracking.task.demo

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.tracking.task.BuildConfig
import com.tracking.task.mvi.BaseViewModel
import org.json.JSONArray
import org.json.JSONObject

class DemoViewModel : BaseViewModel<DemoState, DemoEffect, DemoEvent>() {

    private var databaseReference: DatabaseReference = FirebaseDatabase.getInstance(
        BuildConfig.FIREBASE_URL).reference.child("locations").child("paths")

    override fun createInitialState(): DemoState = DemoState(
        isLoading = true,
        locationsResult = ""
    )

    override fun handleEvent(event: DemoEvent) {
        when (event) {
            DemoEvent.GetLocationsData -> getLocationsData()
        }
    }

    private fun getLocationsData() {
        setState { copy(isLoading = true) }
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {

                if (dataSnapshot.exists() && dataSnapshot.hasChildren()) {
                    val pathsArray = JSONArray()
                    for (pathSnapshot in dataSnapshot.children) {
                        val pathArray = JSONArray()
                        for (pointSnapshot in pathSnapshot.children) {
                            val latitude = pointSnapshot.child("0").getValue(Double::class.java)
                            val longitude = pointSnapshot.child("1").getValue(Double::class.java)
                            val pointArray = JSONArray()
                            pointArray.put(latitude)
                            pointArray.put(longitude)
                            pathArray.put(pointArray)
                        }
                        pathsArray.put(pathArray)
                    }
                    val spatialReference = JSONObject()
                    spatialReference.put("wkid", 102100)
                    spatialReference.put("latestWkid", 3857)

                    val locationsObject = JSONObject()
                    locationsObject.put("paths", pathsArray)
                    locationsObject.put("spatialReference", spatialReference)

                    val jsonString = locationsObject.toString()
                    println("checkflow $jsonString")

                    setState {
                        copy(isLoading = false, locationsResult = jsonString)
                    }


                } else {
                    setState { copy(isLoading = false) }
                    sendEffect(DemoEffect.ErrorWithGetLocations("No data found"))
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                setState { copy(isLoading = false) }
                sendEffect(DemoEffect.ErrorWithGetLocations(databaseError.message))
            }
        })
    }

}